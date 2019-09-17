package org.zstack.test.integration.network.l3network

import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.IpStatisticData
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.NetworkUtils

/**
 * Created by Qi Le on 2019/9/12
 */
class GetL3IpStatisticCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.OneIpL3Network()
    }

    @Override
    void test() {
        env.create {
            testGetIpStatistics()
        }
    }

    void testGetIpStatistics() {
        InstanceOfferingInventory instOffer = env.inventoryByName("instanceOffering")
        ImageInventory img = env.inventoryByName("image1")
        L3NetworkInventory l3Net = env.inventoryByName("pubL3")

        createVip {
            name = "vip-1"
            l3NetworkUuid = l3Net.uuid
            requiredIp = "12.16.10.22"
        }

        createVip {
            name = "vip-2"
            l3NetworkUuid = l3Net.uuid
            requiredIp = "12.16.10.24"
        }

        VmInstanceInventory vm1 = createVmInstance {
            name = "vm1"
            instanceOfferingUuid = instOffer.uuid
            imageUuid = img.uuid
            l3NetworkUuids = Arrays.asList(l3Net.uuid)
        }

        VmInstanceInventory vm2 = createVmInstance {
            name = "vm2"
            instanceOfferingUuid = instOffer.uuid
            imageUuid = img.uuid
            l3NetworkUuids = Arrays.asList(l3Net.uuid)
        }

        List<VmInstanceInventory> vms = new ArrayList<>()

        if (NetworkUtils.ipv4StringToLong(vm1.getVmNics().first().ip) < NetworkUtils.ipv4StringToLong(vm2.getVmNics().first().ip)) {
            vms.add(vm1)
            vms.add(vm2)
        } else {
            vms.add(vm2)
            vms.add(vm1)
        }

        def res = getL3NetworkIpStatistic {
            l3NetworkUuid = l3Net.uuid
            replyWithCount = true
        }

        assert res.total == 4

        List<IpStatisticData> IpData = res.ipStatistics

        for (IpStatisticData d : IpData) {
            if (d.vipUuid != null) {
                if (d.vipName == "vip-1") {
                    assert d.ip == "12.16.10.22"
                } else {
                    assert d.ip == "12.16.10.24"
                }
            }
        }

        res = getL3NetworkIpStatistic {
            l3NetworkUuid = l3Net.uuid
            resourceType = "VM"
            replyWithCount = true
        }

        assert res.total == 2

        IpData = res.ipStatistics

        assert IpData.first().vmInstanceUuid == vms.first().uuid
        assert IpData.get(1).vmInstanceUuid == vms.get(1).uuid

        res = getL3NetworkIpStatistic {
            l3NetworkUuid = l3Net.uuid
            resourceType = "Vip"
            replyWithCount = true
        }

        assert res.total == 2

        IpData = res.ipStatistics

        assert IpData.first().vipName == "vip-1"
        assert IpData.first().ownerName == "admin"
        assert IpData.get(1).vipName == "vip-2"
        assert IpData.get(1).ownerName == "admin"
    }
}
