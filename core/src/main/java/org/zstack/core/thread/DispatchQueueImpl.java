package org.zstack.core.thread;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.signal.SignalHandler;
import org.zstack.core.signal.Signals;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
class DispatchQueueImpl implements DispatchQueue, SignalHandler {
    private static final CLogger logger = Utils.getLogger(DispatchQueueImpl.class);

	@Autowired
	ThreadFacade _threadFacade;

	private final HashMap<String, SyncTaskQueueWrapper> syncTasks = new HashMap<String, SyncTaskQueueWrapper>();
	private final HashMap<String, ChainTaskQueueWrapper> chainTasks = new HashMap<String, ChainTaskQueueWrapper>();
	private static final CLogger _logger = CLoggerImpl.getLogger(DispatchQueueImpl.class);


	public void init() {
        Platform.handleSignal(Signals.USR2, this);
	}

	public void destroy() {
	}

    @Override
    public void handlePOSIXSignal() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n================= BEGIN TASK QUEUE DUMP ================");
        sb.append("\nASYNC TASK QUEUE DUMP:");
        sb.append(String.format("\nTASK QUEUE NUMBER: %s\n", chainTasks.size()));
        List<String> asyncTasks = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, ChainTaskQueueWrapper> e : chainTasks.entrySet()) {
            StringBuilder tb = new StringBuilder(String.format("\nQUEUE SYNC SIGNATURE: %s", e.getKey()));
            ChainTaskQueueWrapper w = e.getValue();
            tb.append(String.format("\nPENDING TASK NUMBER:%s", w.queue.size()));
            for (int i=0; i<w.queue.size(); i++) {
                ChainFuture cf = (ChainFuture) w.queue.get(i);
                if (i == 0) {
                    sb.append(String.format("\nCURRENT TASK[NAME: %s, CLASS: %s EXECUTION TIME: %s secs, INDEX: %s]",
                            cf.getTask().getName(), cf.getTask().getClass(), TimeUnit.MILLISECONDS.toSeconds(now - cf.getCreatedTime()), i));
                } else {
                    sb.append(String.format("\nPENDING TASK[NAME: %s, CLASS: %s EXECUTION TIME: %s secs, INDEX: %s]",
                            cf.getTask().getName(), cf.getTask().getClass(), TimeUnit.MILLISECONDS.toSeconds(now - cf.getCreatedTime()), i));
                }
            }
            asyncTasks.add(tb.toString());
        }
        sb.append(StringUtils.join(asyncTasks, "\n"));
        sb.append("\n================= END TASK QUEUE DUMP ==================\n");
        logger.debug(sb.toString());
    }

    private class SyncTaskFuture<T> extends AbstractFuture<T> {
        public SyncTaskFuture(SyncTask<T> task) {
            super(task);
        }

        private SyncTask getTask() {
            return (SyncTask)task;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancel();
            return true;
        }

        void run() {
            if (isCancelled()) {
                return;
            }

            try {
                ret = (T) getTask().call();
            } catch (Throwable t) {
                _logger.warn(String.format("unhandled exception happened when calling sync task[name:%s, class:%s]",
                        getTask().getName(), getTask().getClass().getName()), t);
                exception = t;
            }

            done();
        }

        String getSyncSignature() {
            return getTask().getSyncSignature();
        }

        int getSyncLevel() {
            return getTask().getSyncLevel();
        }

        String getName() {
            return getTask().getName();
        }
    }

    private class SyncTaskQueueWrapper {
        ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
        AtomicInteger counter = new AtomicInteger(0);
        int maxThreadNum = -1;
        String syncSignature;

        void addTask(SyncTaskFuture task) {
            queue.offer(task);
            if (maxThreadNum == -1) {
                maxThreadNum = task.getSyncLevel();
            }
            if (syncSignature == null) {
                syncSignature = task.getSyncSignature();
            }
        }

        void startThreadIfNeeded() {
            if (counter.get() >= maxThreadNum) {
                return;
            }

            counter.incrementAndGet();
            _threadFacade.submit(new Task<Void>() {
                @Override
                public String getName() {
                    return syncSignature;
                }

                void run() {
                    SyncTaskFuture stask;
                    while (true) {
                        while ((stask = (SyncTaskFuture) queue.poll()) != null) {
                            stask.run();
                        }

                        synchronized (syncTasks) {
                            if (queue.isEmpty()) {
                                if (counter.decrementAndGet() == 0) {
                                    syncTasks.remove(syncSignature);
                                }

                                break;
                            }
                        }
                    }

                }

                @Override
                public Void call() throws Exception {
                    run();
                    return null;
                }
            });
        }
    }

	private <T> Future<T> doSyncSubmit(final SyncTask<T> syncTask) {
		assert syncTask.getSyncSignature() != null : "How can you submit a sync task without sync signature ???";

        SyncTaskFuture f;
        synchronized (syncTasks) {
            SyncTaskQueueWrapper wrapper = syncTasks.get(syncTask.getSyncSignature());
            if (wrapper == null) {
                wrapper = new SyncTaskQueueWrapper();
                syncTasks.put(syncTask.getSyncSignature(), wrapper);
            }
            f = new SyncTaskFuture(syncTask);
            wrapper.addTask(f);
            wrapper.startThreadIfNeeded();
        }

		return f;
	}

	@Override
	public <T> Future<T> syncSubmit(SyncTask<T> task) {
		if (task.getSyncLevel() <= 0) {
			return _threadFacade.submit(task);
		} else {
			return doSyncSubmit(task);
		}
	}


    class ChainFuture extends  AbstractFuture {
        private AtomicBoolean isNextCalled = new AtomicBoolean(false);
        private long createdTime = System.currentTimeMillis();

        public long getCreatedTime() {
            return createdTime;
        }

        public ChainFuture(ChainTask task) {
            super(task);
        }

        private ChainTask getTask() {
            return (ChainTask)task;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancel();
            return true;
        }

        private void callNext(SyncTaskChain chain) {
            if (!isNextCalled.compareAndSet(false, true)) {
                return;
            }

            chain.next();
        }

        public void run(final SyncTaskChain chain) {
            if (isCancelled()) {
                callNext(chain);
                return;
            }

            try {
                getTask().run(new SyncTaskChain() {
                    @Override
                    public void next() {
                        try {
                            done();
                        } finally {
                            callNext(chain);
                        }
                    }
                });
            } catch (Throwable t) {
                try {
                    _logger.warn(String.format("unhandled exception happened when calling %s", task.getClass().getName()), t);
                    done();
                } finally {
                    callNext(chain);
                }
            }
        }

        public int getSyncLevel() {
            return getTask().getSyncLevel();
        }

        public String getSyncSignature() {
            return getTask().getSyncSignature();
        }
    }

    private class ChainTaskQueueWrapper {
        LinkedList queue = new LinkedList();
        AtomicInteger counter = new AtomicInteger(0);
        int maxThreadNum = -1;
        String syncSignature;

        void addTask(ChainFuture task) {
            queue.offer(task);
            if (maxThreadNum == -1) {
                maxThreadNum = task.getSyncLevel();
            }
            if (syncSignature == null) {
                syncSignature = task.getSyncSignature();
            }
        }

        void startThreadIfNeeded() {
            if (counter.get() >= maxThreadNum) {
                return;
            }

            counter.incrementAndGet();
            _threadFacade.submit(new Task<Void>() {
                @Override
                public String getName() {
                    return "sync-chain-thread";
                }

                // start a new thread every time to avoid stack overflow
                @AsyncThread
                private void runQueue() {
                    ChainFuture cf;
                    synchronized (chainTasks) {
                        // use peek() instead of poll() not to remove the task from the queue
                        // the task will be removed by poll when it is finished
                        cf = (ChainFuture) queue.peek();
                        if (cf == null) {
                            if (counter.decrementAndGet() == 0) {
                                chainTasks.remove(syncSignature);
                            }

                            return;
                        }
                    }

                    cf.run(new SyncTaskChain() {
                        @Override
                        public void next() {
                            // use poll to removes the the finished task from the queue
                            queue.poll();
                            runQueue();
                        }
                    });
                }

                @Override
                public Void call() throws Exception {
                    runQueue();
                    return null;
                }
            });
        }
    }


	private <T> Future<T> doChainSyncSubmit(final ChainTask task) {
        assert task.getSyncSignature() != null : "How can you submit a chain task without sync signature ???";
        DebugUtils.Assert(task.getSyncLevel() >= 1, String.format("getSyncLevel() must return more than 1"));

        synchronized (chainTasks) {
            final String signature = task.getSyncSignature();
            ChainTaskQueueWrapper wrapper = chainTasks.get(signature);
            if (wrapper == null) {
                wrapper = new ChainTaskQueueWrapper();
                chainTasks.put(signature, wrapper);
            }

            ChainFuture cf = new ChainFuture(task);
            wrapper.addTask(cf);
            wrapper.startThreadIfNeeded();
            return cf;
        }
    }
	

    @Override
    public Future<Void> chainSubmit(ChainTask task) {
        return doChainSyncSubmit(task);
    }

    @Override
    public Map<String, SyncTaskStatistic> getSyncTaskStatistics() {
        Map<String, SyncTaskStatistic> ret = new HashMap<String, SyncTaskStatistic>();
        for (SyncTaskQueueWrapper wrapper : syncTasks.values()) {
            SyncTaskStatistic statistic = new SyncTaskStatistic(
                    wrapper.syncSignature,
                    wrapper.maxThreadNum,
                    wrapper.counter.intValue(),
                    wrapper.queue.size()
            );
            ret.put(statistic.getSyncSignature(), statistic);

            logger.warn(JSONObjectUtil.toJsonString(statistic));
        }

        return ret;
    }

    @Override
    public Map<String, ChainTaskStatistic> getChainTaskStatistics() {
        Map<String, ChainTaskStatistic> ret = new HashMap<String, ChainTaskStatistic>();
        for (ChainTaskQueueWrapper wrapper : chainTasks.values()) {
            ChainTaskStatistic statistic = new ChainTaskStatistic(
                    wrapper.syncSignature,
                    wrapper.maxThreadNum,
                    wrapper.counter.intValue(),
                    wrapper.queue.size()
            );
            ret.put(statistic.getSyncSignature(), statistic);
        }
        return ret;
    }
}
