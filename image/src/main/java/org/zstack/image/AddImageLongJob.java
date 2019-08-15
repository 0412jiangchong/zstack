package org.zstack.image;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Constants;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.*;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.longjob.LongJob;
import org.zstack.longjob.LongJobUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;


/**
 * Created by on camile 2018/2/2.
 */
@LongJobFor(APIAddImageMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AddImageLongJob implements LongJob {
    private static final CLogger logger = Utils.getLogger(AddImageLongJob.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    protected String auditResourceUuid;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        AddImageMsg msg = JSONObjectUtil.toObject(job.getJobData(), AddImageMsg.class);
        if (msg.getResourceUuid() == null) {
            msg.setResourceUuid(Platform.getUuid());
            job.setJobData(JSONObjectUtil.toJsonString(msg));
            dbf.update(job);
        }
        bus.makeLocalServiceId(msg, ImageConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AddImageReply r = reply.castReply();
                    APIAddImageEvent evt = new APIAddImageEvent(ThreadContext.get(Constants.THREAD_CONTEXT_API));

                    auditResourceUuid = r.getInventory().getUuid();
                    evt.setInventory(r.getInventory());
                    completion.success(evt);
                } else {
                    auditResourceUuid = msg.getResourceUuid();
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void resume(LongJobVO job) {
        AddImageMsg msg = JSONObjectUtil.toObject(job.getJobData(), AddImageMsg.class);
        ImageDeletionMsg dmsg = new ImageDeletionMsg();
        dmsg.setImageUuid(msg.getResourceUuid());
        dmsg.setForceDelete(true);
        dmsg.setBackupStorageUuids(msg.getBackupStorageUuids());
        bus.makeTargetServiceIdByResourceUuid(dmsg, ImageConstant.SERVICE_ID, dmsg.getImageUuid());
        bus.send(dmsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("delete image [%s] failed after management node restarted", msg.getResourceUuid()));
                }

                LongJobUtils.updateByUuid(job.getUuid(), vo -> {
                    vo.setJobResult("Failed because management node restarted.");
                    vo.setState(LongJobState.Failed);
                });
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, Completion completion) {
        // TODO
        completion.fail(Platform.operr("not supported"));
    }

    @Override
    public Class getAuditType() {
        return ImageVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return auditResourceUuid;
    }
}
