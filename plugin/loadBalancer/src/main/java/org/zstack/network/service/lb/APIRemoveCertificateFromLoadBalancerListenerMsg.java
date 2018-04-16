package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by shixin on 03/26/2018.
 */
@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/load-balancers/listeners/{listenerUuid}/certificate",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveCertificateFromLoadBalancerListenerEvent.class
)
public class APIRemoveCertificateFromLoadBalancerListenerMsg extends APIMessage implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = CertificateVO.class, checkAccount = true, operationTarget = true, nonempty = true)
    private String certificateUuid;
    @APIParam(resourceType = LoadBalancerListenerVO.class, checkAccount = true, operationTarget = true)
    private String listenerUuid;
    @APINoSee
    private String loadBalancerUuid;

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    public String getCertificateUuid() {
        return certificateUuid;
    }

    public void setCertificateUuid(String certificateUuid) {
        this.certificateUuid = certificateUuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
 
    public static APIRemoveCertificateFromLoadBalancerListenerMsg __example__() {
        APIRemoveCertificateFromLoadBalancerListenerMsg msg = new APIRemoveCertificateFromLoadBalancerListenerMsg();

        msg.setCertificateUuid(uuid());
        msg.setListenerUuid(uuid());
        msg.setLoadBalancerUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Remove certificate[uuid:%s] from the loadbalancer listener[uuid:%s] ", certificateUuid, listenerUuid)
                            .resource(listenerUuid, LoadBalancerListenerVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIRemoveCertificateFromLoadBalancerListenerMsg)msg).getLoadBalancerUuid(), LoadBalancerVO.class);
    }
}
