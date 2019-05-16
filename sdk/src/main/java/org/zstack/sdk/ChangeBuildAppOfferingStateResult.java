package org.zstack.sdk;

import org.zstack.sdk.BuildAppOfferingInventory;

public class ChangeBuildAppOfferingStateResult {
    public BuildAppOfferingInventory inventory;
    public void setInventory(BuildAppOfferingInventory inventory) {
        this.inventory = inventory;
    }
    public BuildAppOfferingInventory getInventory() {
        return this.inventory;
    }

}
