package org.zstack.test.integration.longjob

import com.google.gson.Gson
import org.zstack.core.db.Q
import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg
import org.zstack.header.image.ImagePlatform
import org.zstack.header.image.ImageVO
import org.zstack.header.longjob.LongJobVO
import org.zstack.header.longjob.LongJobVO_
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.LongJobInventory
import org.zstack.header.longjob.LongJobState
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.storage.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by camile on 2/6/18.
 */
class CreateRootVolumeTemplateLongJobCase extends SubCase {
    EnvSpec env
    Gson gson
    BackupStorageInventory bs
    VmInstanceInventory vm

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testApiMessageValidator()
            testCreateRootVolumeTemplate()
        }
    }

    void testApiMessageValidator() {
        bs = env.inventoryByName("sftp") as BackupStorageInventory
        vm = env.inventoryByName("vm") as VmInstanceInventory
        gson = new Gson()

        APICreateRootVolumeTemplateFromRootVolumeMsg msg = new APICreateRootVolumeTemplateFromRootVolumeMsg()
        msg.name = "test"
        msg.rootVolumeUuid = vm.rootVolumeUuid
        msg.platform = ImagePlatform.Linux.toString() + "test"
        msg.backupStorageUuids = Collections.singletonList(bs.uuid)

        expect(AssertionError.class) {
            submitLongJob {
                sessionId = adminSession()
                jobName = "APICreateRootVolumeTemplateFromRootVolumeMsg"
                jobData = gson.toJson(msg)
            }
        }
    }

    void testCreateRootVolumeTemplate() {
        int oldSize = Q.New(ImageVO.class).list().size()
        int flag = 0
        String _description = "my-test"

        env.afterSimulator(LocalStorageKvmBackend.GET_VOLUME_SIZE) { Object response ->
            //SyncVolumeSizeMsg
            LongJobVO vo = Q.New(LongJobVO.class).eq(LongJobVO_.description, _description).find()
            assert vo.state == LongJobState.Running || vo.state == LongJobState.Waiting
            flag += 1
            return response
        }

        env.afterSimulator(LocalStorageKvmBackend.CREATE_TEMPLATE_FROM_VOLUME) { Object response ->
            //CreateTemplateFromVmRootVolumeMsg
            LongJobVO vo = Q.New(LongJobVO.class).eq(LongJobVO_.description, "my-test").find()
            assert vo.state == LongJobState.Running
            flag += 1
            return response
        }

        env.afterSimulator(SftpBackupStorageConstant.GET_IMAGE_SIZE) { Object response ->
            //SyncImageSizeMsg -> BackupStorageAskInstallPathMsg
            LongJobVO vo = Q.New(LongJobVO.class).eq(LongJobVO_.description, "my-test").find()
            assert vo.state == LongJobState.Running
            flag += 1
            return response
        }

        APICreateRootVolumeTemplateFromRootVolumeMsg msg = new APICreateRootVolumeTemplateFromRootVolumeMsg()
        msg.name = "test"
        msg.rootVolumeUuid = vm.rootVolumeUuid
        msg.platform = ImagePlatform.Linux.toString()
        msg.backupStorageUuids = Collections.singletonList(bs.uuid)

        LongJobInventory jobInv = submitLongJob {
            sessionId = adminSession()
            jobName = "APICreateRootVolumeTemplateFromRootVolumeMsg"
            jobData = gson.toJson(msg)
            description = _description
        } as LongJobInventory

        assert jobInv.jobName == "APICreateRootVolumeTemplateFromRootVolumeMsg"
        assert jobInv.state == org.zstack.sdk.LongJobState.Running

        retryInSecs() {
            LongJobVO job = dbFindByUuid(jobInv.getUuid(), LongJobVO.class)
            assert job.state == LongJobState.Succeeded
        }

        int newSize = Q.New(ImageVO.class).list().size()
        assert newSize > oldSize
        assert 3 == flag
    }
}
