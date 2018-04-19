package org.zstack.header.cluster;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Created by GuoYi on 3/12/18
 */
@RestRequest(
        path = "/clusters/{uuid}/actions",
        responseClass = APIUpdateClusterOSEvent.class,
        isAction = true,
        method = HttpMethod.PUT
)
public class APIUpdateClusterOSMsg extends APICreateMessage implements ClusterMessage {
    @APIParam(resourceType = ClusterVO.class)
    private String uuid;
    @APIParam(required = false, nonempty = true)
    private List<String> excludePackages;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getExcludePackages() {
        return excludePackages;
    }

    public void setExcludePackages(List<String> excludePackages) {
        this.excludePackages = excludePackages;
    }

    @Override
    public String getClusterUuid() {
        return uuid;
    }

    public static APIUpdateClusterOSMsg __example__() {
        APIUpdateClusterOSMsg msg = new APIUpdateClusterOSMsg();
        msg.setUuid(uuid());
        msg.setExcludePackages(Arrays.asList("kernel", "systemd*"));
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Cluster OS Updated").resource(uuid, ClusterVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
