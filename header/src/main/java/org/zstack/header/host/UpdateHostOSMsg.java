package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 3/12/18
 */
public class UpdateHostOSMsg extends NeedReplyMessage implements HostMessage {
    private String uuid;
    private String clusterUuid;
    private String excludePackages;
    private boolean enableExperimentalRepo = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getExcludePackages() {
        return excludePackages;
    }

    public void setExcludePackages(String excludePackages) {
        this.excludePackages = excludePackages;
    }

    public boolean isEnableExperimentalRepo() {
        return enableExperimentalRepo;
    }

    public void setEnableExperimentalRepo(boolean enableExperimentalRepo) {
        this.enableExperimentalRepo = enableExperimentalRepo;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }
}
