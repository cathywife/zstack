package org.zstack.storage.volume;

import org.zstack.header.volume.VolumeVO;

public interface CreateVolumeExtensionPoint {
    VolumeVO beforeCreateVolume(VolumeVO volumeVO);
}
