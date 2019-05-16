package org.zstack.sdk;

import org.zstack.sdk.RawApplicationInventory;

public class ChangeRawAppStateResult {
    public RawApplicationInventory inventory;
    public void setInventory(RawApplicationInventory inventory) {
        this.inventory = inventory;
    }
    public RawApplicationInventory getInventory() {
        return this.inventory;
    }

}
