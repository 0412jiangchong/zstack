package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.TagUtils;
import org.zstack.utils.network.IPv6NetworkUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by xing5 on 2016/5/25.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StaticIpOperator {
    @Autowired
    private DatabaseFacade dbf;

    public Map<String, String> getStaticIpbyVmUuid(String vmUuid) {
        Map<String, String> ret = new HashMap<String, String>();

        List<Map<String, String>> tokenList = VmSystemTags.STATIC_IP.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            String l3Uuid = tokens.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
            String ip = tokens.get(VmSystemTags.STATIC_IP_TOKEN);
            ip = IPv6NetworkUtils.ipv6TagValueToAddress(ip);
            ret.put(l3Uuid, ip);
        }

        return ret;
    }

    public Map<String, String> getStaticIpbySystemTag(List<String> systemTags) {
        Map<String, String> ret = new HashMap<>();

        if (systemTags == null) {
            return ret;
        }

        for (String sysTag : systemTags) {
            if(!VmSystemTags.STATIC_IP.isMatch(sysTag)) {
                continue;
            }

            Map<String, String> token = TagUtils.parse(VmSystemTags.STATIC_IP.getTagFormat(), sysTag);
            String l3Uuid = token.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
            String ip = token.get(VmSystemTags.STATIC_IP_TOKEN);
            ip = IPv6NetworkUtils.ipv6TagValueToAddress(ip);
            ret.put(l3Uuid, ip);
        }

        return ret;
    }

    public void setStaticIp(String vmUuid, String l3Uuid, String ip) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.select(SystemTagVO_.uuid);
        q.add(SystemTagVO_.resourceType, Op.EQ, VmInstanceVO.class.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.EQ, vmUuid);
        q.add(SystemTagVO_.tag, Op.LIKE, TagUtils.tagPatternToSqlPattern(VmSystemTags.STATIC_IP.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid))
        )));
        final String tagUuid = q.findValue();

        /* '::' is token used by systemtag, replace with "--" */
        ip = IPv6NetworkUtils.ipv6AddessToTagValue(ip);
        if (tagUuid == null) {
            SystemTagCreator creator = VmSystemTags.STATIC_IP.newSystemTagCreator(vmUuid);
            creator.setTagByTokens(map(
                    e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                    e(VmSystemTags.STATIC_IP_TOKEN, ip)
            ));
            creator.create();
        } else {
            VmSystemTags.STATIC_IP.updateByTagUuid(tagUuid, VmSystemTags.STATIC_IP.instantiateTag(map(
                    e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                    e(VmSystemTags.STATIC_IP_TOKEN, ip)
            )));
        }
    }

    public void deleteStaticIpByVmUuidAndL3Uuid(String vmUuid, String l3Uuid) {
        VmSystemTags.STATIC_IP.delete(vmUuid, TagUtils.tagPatternToSqlPattern(VmSystemTags.STATIC_IP.instantiateTag(
                map(e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid))
        )));
    }

    public void deleteStaticIpByL3NetworkUuid(String l3Uuid) {
        VmSystemTags.STATIC_IP.delete(null, VmSystemTags.STATIC_IP.instantiateTag(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3Uuid),
                e(VmSystemTags.STATIC_IP_TOKEN, "%")
        )));
    }
}
