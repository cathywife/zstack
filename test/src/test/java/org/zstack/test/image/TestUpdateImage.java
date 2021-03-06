package org.zstack.test.image;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.simulator.storage.backup.SimulatorBackupStorageDetails;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

public class TestUpdateImage {
    CLogger logger = Utils.getLogger(TestUpdateImage.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("Simulator.xml").addXml("BackupStorageManager.xml")
                .addXml("ImageManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        SimulatorBackupStorageDetails ss = new SimulatorBackupStorageDetails();
        ss.setTotalCapacity(SizeUnit.GIGABYTE.toByte(100));
        ss.setUsedCapacity(0);
        ss.setUrl("nfs://simulator/backupstorage/");
        BackupStorageInventory inv = api.createSimulatorBackupStorage(1, ss).get(0);
        BackupStorageVO vo = dbf.findByUuid(inv.getUuid(), BackupStorageVO.class);
        Assert.assertNotNull(vo);

        ImageInventory iinv = new ImageInventory();
        iinv.setName("Test Image");
        iinv.setDescription("Test Image");
        iinv.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        iinv.setGuestOsType("Window7");
        iinv.setFormat("simulator");
        iinv.setUrl("http://zstack.org/download/win7.qcow2");
        iinv = api.addImage(iinv, inv.getUuid());

        iinv.setName("1");
        iinv.setDescription("xxx");
        iinv.setGuestOsType("yyy");
        iinv.setFormat("raw");
        iinv.setMediaType(ImageMediaType.DataVolumeTemplate.toString());
        iinv.setSystem(true);
        iinv.setPlatform(ImagePlatform.Paravirtualization.toString());

        iinv = api.updateImage(iinv);
        Assert.assertEquals("1", iinv.getName());
        Assert.assertEquals("xxx", iinv.getDescription());
        Assert.assertEquals("yyy", iinv.getGuestOsType());
        Assert.assertTrue(iinv.isSystem());
        Assert.assertEquals(ImagePlatform.Paravirtualization.toString(), iinv.getPlatform());
        Assert.assertEquals("raw", iinv.getFormat());
        Assert.assertEquals(ImageMediaType.DataVolumeTemplate.toString(), iinv.getMediaType());
    }
}
