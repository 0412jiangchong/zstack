package org.zstack.header.storage.snapshot.group;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/7/9.
 */
@RestResponse(allTo = "results")
public class APIRevertVmFromSnapshotGroupEvent extends APIEvent {
    private List<RevertSnapshotGroupResult> results;

    public APIRevertVmFromSnapshotGroupEvent(String apiId) {
        super(apiId);
    }

    public APIRevertVmFromSnapshotGroupEvent() {
        super();
    }

    public List<RevertSnapshotGroupResult> getResults() {
        return results;
    }

    public void setResults(List<RevertSnapshotGroupResult> results) {
        this.results = results;
    }
}
