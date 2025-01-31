package org.zstack.network.l3;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class NetworkGlobalProperty {
    @GlobalProperty(name = "skip.ipv6", defaultValue = "false")
    public static boolean SKIP_IPV6;

    @GlobalProperty(name = "chssis.asset.tag", defaultValue = "www.zstack.io")
    public static String CHASSIS_ASSET_TAG;
}
