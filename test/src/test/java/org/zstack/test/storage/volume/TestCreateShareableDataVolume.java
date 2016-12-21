package org.zstack.test.storage.volume;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.APICreateDataVolumeEvent;
import org.zstack.header.volume.APICreateDataVolumeMsg;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.mevoco.APIQueryShareableVolumeVmInstanceRefMsg;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO_;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.Arrays;

public class TestCreateShareableDataVolume {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/volume/TestCreateDataVolume.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        DiskOfferingInventory diskOfferingInventory = deployer.diskOfferings.get("TestDataDiskOffering");
        PrimaryStorageInventory ps1 = deployer.primaryStorages.get("TestPrimaryStorage1");

        ApiSender sender = new ApiSender();
        sender.setTimeout(1200);

        VolumeInventory vol;
        {
            APICreateDataVolumeMsg msg = new APICreateDataVolumeMsg();
            msg.setSession(session);
            msg.setPrimaryStorageUuid(ps1.getUuid());
            msg.setName("shareable volume");
            msg.setDiskOfferingUuid(diskOfferingInventory.getUuid());
            String tag = MevocoSystemTags.SHAREABLE_DEVICE.getTagFormat();
            msg.setSystemTags(Arrays.asList(tag));
            APICreateDataVolumeEvent e = sender.send(msg, APICreateDataVolumeEvent.class);
            vol = e.getInventory();

            Assert.assertTrue(vol.isShareable());
        }

        {
            APIQueryShareableVolumeVmInstanceRefMsg msg = new APIQueryShareableVolumeVmInstanceRefMsg();
            msg.setSession(session);
            msg.addQueryCondition(ShareableVolumeVmInstanceRefVO_.volumeUuid.getName(), QueryOp.EQ, vol.getUuid());
            Assert.assertTrue(0 == api.queryCount(msg, session));
        }

        api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        {
            SimpleQuery<ShareableVolumeVmInstanceRefVO> q = dbf.createQuery(ShareableVolumeVmInstanceRefVO.class);
            Assert.assertTrue(q.count() == 1);
        }

        {
            APIQueryShareableVolumeVmInstanceRefMsg msg = new APIQueryShareableVolumeVmInstanceRefMsg();
            msg.setSession(session);
            msg.addQueryCondition(ShareableVolumeVmInstanceRefVO_.volumeUuid.getName(), QueryOp.EQ, vol.getUuid());
            Assert.assertTrue(1 == api.queryCount(msg, session));
        }

        api.detachVolumeFromVmEx(vol.getUuid(), vm.getUuid(), null);
        {
            SimpleQuery<ShareableVolumeVmInstanceRefVO> q = dbf.createQuery(ShareableVolumeVmInstanceRefVO.class);
            Assert.assertTrue(q.count() == 0);
        }

        {
            APIQueryShareableVolumeVmInstanceRefMsg msg = new APIQueryShareableVolumeVmInstanceRefMsg();
            msg.setSession(session);
            msg.addQueryCondition(ShareableVolumeVmInstanceRefVO_.volumeUuid.getName(), QueryOp.EQ, vol.getUuid());
            Assert.assertTrue(0 == api.queryCount(msg, session));
        }
    }
}
