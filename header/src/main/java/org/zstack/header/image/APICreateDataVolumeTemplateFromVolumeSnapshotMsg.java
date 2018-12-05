package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 */
@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/images/data-volume-templates/from/volume-snapshots/{snapshotUuid}",
        responseClass = APICreateDataVolumeTemplateFromVolumeSnapshotEvent.class,
        method = HttpMethod.POST,
        parameterName = "params"
)
@APIDefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class APICreateDataVolumeTemplateFromVolumeSnapshotMsg extends APICreateMessage implements APIAuditor {
    @APIParam(resourceType = VolumeSnapshotVO.class, checkAccount = true, operationTarget = true)
    private String snapshotUuid;
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(nonempty = true, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }
 
    public static APICreateDataVolumeTemplateFromVolumeSnapshotMsg __example__() {
        APICreateDataVolumeTemplateFromVolumeSnapshotMsg msg = new APICreateDataVolumeTemplateFromVolumeSnapshotMsg();

        msg.setName("My Data Volume Template");
        msg.setBackupStorageUuids(Collections.singletonList(uuid()));
        msg.setSnapshotUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Created from a volume snapshot[uuid:%s]", snapshotUuid)
                            .resource(((APICreateDataVolumeTemplateFromVolumeSnapshotEvent)evt).getInventory().getUuid(),
                            ImageVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateDataVolumeTemplateFromVolumeSnapshotEvent)rsp).getInventory().getUuid() : "", ImageVO.class);
    }
}
