package org.zstack.storage.ceph.primary;

import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.storage.primary.APIQueryPrimaryStorageReply;

/**
 * Created by frank on 8/6/2015.
 */
@AutoQuery(replyClass = APIQueryPrimaryStorageReply.class, inventoryClass = CephPrimaryStorageInventory.class)
public class APIQueryCephPrimaryStorageMsg extends APIQueryMessage {
}
