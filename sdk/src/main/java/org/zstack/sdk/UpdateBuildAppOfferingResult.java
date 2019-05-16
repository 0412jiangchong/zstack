package org.zstack.sdk;

import org.zstack.sdk.BuildAppOfferingInventory;

public class UpdateBuildAppOfferingResult {
    public BuildAppOfferingInventory inventory;
    public void setInventory(BuildAppOfferingInventory inventory) {
        this.inventory = inventory;
    }
    public BuildAppOfferingInventory getInventory() {
        return this.inventory;
    }

}
