package org.zstack.core.config.resourceconfig;

/**
 * Created by MaJin on 2019/2/26.
 */
public interface ResourceConfigDeleteExtensionPoint {
    void deleteResourceConfig(ResourceConfig config, String resourceUuid, String originValue);
}
