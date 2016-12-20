package org.zstack.header.vm;

import org.zstack.header.volume.VolumeInventory;

import java.util.List;

/**
 * Created by miao on 12/20/16.
 */
public interface GetVolumeAttachedVmUuidsExtensionPoint {
    List<String> GetVolumeAttachedVmUuids(VolumeInventory volumeInventory);
}
