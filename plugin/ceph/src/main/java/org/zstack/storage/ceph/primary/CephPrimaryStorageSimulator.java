package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.simulator.AsyncRESTReplyer;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase.*;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig.CephPrimaryStorageConfig;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by frank on 7/28/2015.
 */
@Controller
public class CephPrimaryStorageSimulator {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private CephPrimaryStorageSimulatorConfig config;

    private Map<String, Long> bitSizeMap = new HashMap<String, Long>();

    AsyncRESTReplyer replyer = new AsyncRESTReplyer();

    private CephPrimaryStorageConfig getConfig(AgentCommand cmd) {
        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.select(BackupStorageVO_.name);
        q.add(BackupStorageVO_.uuid, Op.EQ, cmd.getUuid());
        String name = q.findValue();

        CephPrimaryStorageConfig c = config.config.get(name);
        if (c == null) {
            throw new CloudRuntimeException(String.format("cannot find CephPrimaryStorageConfig by name[%s], uuid[%s]", name, cmd.getUuid()));
        }

        return c;
    }

    @RequestMapping(value= CephPrimaryStorageBase.DELETE_POOL_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String deletePool(HttpEntity<String> entity) {
        DeletePoolCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeletePoolCmd.class);
        config.deletePoolCmds.add(cmd);
        replyer.reply(entity, new DeletePoolRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.INIT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String initialize(HttpEntity<String> entity) {
        InitCmd cmd = JSONObjectUtil.toObject(entity.getBody(), InitCmd.class);
        CephPrimaryStorageConfig cpc = getConfig(cmd);

        InitRsp rsp = new InitRsp();
        rsp.fsid = cpc.fsid;
        rsp.userKey = Platform.getUuid();
        rsp.totalCapacity = cpc.totalCapacity;
        rsp.availableCapacity = cpc.availCapacity;
        replyer.reply(entity, rsp);
        return null;
    }

    private void setCapacity(AgentCommand cmd, AgentResponse rsp, long size) {
        CephPrimaryStorageConfig cpc = getConfig(cmd);
        rsp.totalCapacity = cpc.totalCapacity;
        rsp.availableCapacity = cpc.availCapacity + size;
    }

    @RequestMapping(value= CephPrimaryStorageBase.CREATE_VOLUME_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String createEmptyVolume(HttpEntity<String> entity) {
        CreateEmptyVolumeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateEmptyVolumeCmd.class);
        config.createEmptyVolumeCmds.add(cmd);

        CreateEmptyVolumeRsp rsp = new CreateEmptyVolumeRsp();
        setCapacity(cmd, rsp, -cmd.getSize());
        bitSizeMap.put(cmd.getInstallPath(), cmd.getSize());
        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.KVM_CREATE_SECRET_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String createKvmSecret(HttpEntity<String> entity) {
        CreateKvmSecretCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateKvmSecretCmd.class);
        config.createKvmSecretCmds.add(cmd);
        replyer.reply(entity, new KVMAgentCommands.AgentResponse());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.DELETE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String doDelete(HttpEntity<String> entity) {
        DeleteCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteCmd.class);
        config.deleteCmds.add(cmd);
        Long size = bitSizeMap.get(cmd.getInstallPath());
        size = size == null ? 0 : size;

        DeleteRsp rsp = new DeleteRsp();
        setCapacity(cmd, rsp, size);
        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.CREATE_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String createSnapshot(HttpEntity<String> entity) {
        CreateSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateSnapshotCmd.class);
        config.createSnapshotCmds.add(cmd);

        replyer.reply(entity, new CreateSnapshotRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.DELETE_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String deleteSnapshot(HttpEntity<String> entity) {
        DeleteSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteSnapshotCmd.class);
        config.deleteSnapshotCmds.add(cmd);

        replyer.reply(entity, new DeleteSnapshotRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.PROTECT_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String protectSnapshot(HttpEntity<String> entity) {
        ProtectSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ProtectSnapshotCmd.class);
        config.protectSnapshotCmds.add(cmd);

        replyer.reply(entity, new ProtectSnapshotRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.UNPROTECT_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String unprotectSnapshot(HttpEntity<String> entity) {
        UnprotectedSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), UnprotectedSnapshotCmd.class);
        config.unprotectedSnapshotCmds.add(cmd);

        replyer.reply(entity, new UnprotectedSnapshotRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.CLONE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String clone(HttpEntity<String> entity) {
        CloneCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CloneCmd.class);
        config.cloneCmds.add(cmd);

        CloneRsp rsp = new CloneRsp();
        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.FLATTEN_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String flatten(HttpEntity<String> entity) {
        FlattenCmd cmd = JSONObjectUtil.toObject(entity.getBody(), FlattenCmd.class);
        config.flattenCmds.add(cmd);

        replyer.reply(entity, new FlattenRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.CP_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String cp(HttpEntity<String> entity) {
        CpCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CpCmd.class);
        config.cpCmds.add(cmd);

        replyer.reply(entity, new CpRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.SFTP_UPLOAD_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String sftpUpload(HttpEntity<String> entity) {
        SftpUpLoadCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SftpUpLoadCmd.class);
        config.sftpUpLoadCmds.add(cmd);

        replyer.reply(entity, new SftpDownloadCmd());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.SFTP_DOWNLOAD_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String sftpDownload(HttpEntity<String> entity) {
        SftpDownloadCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SftpDownloadCmd.class);
        config.sftpDownloadCmds.add(cmd);

        replyer.reply(entity, new SftpDownloadRsp());
        return null;
    }

    @RequestMapping(value= CephPrimaryStorageBase.ROLLBACK_SNAPSHOT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String rollback(HttpEntity<String> entity) {
        RollbackSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RollbackSnapshotCmd.class);
        config.rollbackSnapshotCmds.add(cmd);

        replyer.reply(entity, new RollbackSnapshotRsp());
        return null;
    }
}
