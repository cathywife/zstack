package org.zstack.storage.ceph.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.storage.ceph.backup.CephBackupStorageBase.*;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig.CephBackupStorageConfig;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

/**
 * Created by frank on 7/28/2015.
 */
@Controller
public class CephBackupStorageSimulator {
    CLogger logger = Utils.getLogger(CephBackupStorageSimulator.class);

    @Autowired
    private CephBackupStorageSimulatorConfig config;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;

    public void reply(HttpEntity<String> entity, Object rsp) {
        String taskUuid = entity.getHeaders().getFirst(RESTConstant.TASK_UUID);
        String callbackUrl = entity.getHeaders().getFirst(RESTConstant.CALLBACK_URL);
        String rspBody = JSONObjectUtil.toJsonString(rsp);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(rspBody.length());
        headers.set(RESTConstant.TASK_UUID, taskUuid);
        HttpEntity<String> rreq = new HttpEntity<String>(rspBody, headers);
        restf.getRESTTemplate().exchange(callbackUrl, HttpMethod.POST, rreq, String.class);
    }

    private CephBackupStorageConfig getConfig(AgentCommand cmd) {
        SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
        q.select(BackupStorageVO_.name);
        q.add(BackupStorageVO_.uuid, Op.EQ, cmd.getUuid());
        String name = q.findValue();

        CephBackupStorageConfig c = config.config.get(name);
        if (c == null) {
            throw new CloudRuntimeException(String.format("cannot find CephBackupStorageConfig by name[%s], uuid[%s]", name, cmd.getUuid()));
        }

        c.name = name;

        return c;
    }

    @RequestMapping(value= CephBackupStorageBase.GET_FACTS, method= RequestMethod.POST)
    public @ResponseBody
    String getFacts(HttpEntity<String> entity) {
        GetFactsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetFactsCmd.class);
        GetFactsRsp rsp = new GetFactsRsp();

        config.getFactsCmds.add(cmd);
        String fsid = config.getFactsCmdFsid.get(cmd.monUuid);
        if (fsid == null) {
            CephBackupStorageConfig c = getConfig(cmd);
            fsid = c.fsid;
        }

        rsp.fsid = fsid;
        rsp.monAddr = config.monAddr.get(cmd.monUuid);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= CephBackupStorageMonBase.PING_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String pingMon(HttpEntity<String> entity) {
        CephBackupStorageMonBase.PingCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CephBackupStorageMonBase.PingCmd.class);
        Boolean success = config.pingCmdSuccess.get(cmd.monUuid);
        CephBackupStorageMonBase.PingRsp rsp = new CephBackupStorageMonBase.PingRsp();
        rsp.success = success == null ? true : success;
        if (!rsp.success) {
            rsp.error = "on purpose";
        }
        rsp.failure = config.pingCmdOperationFailure.get(cmd.monUuid);
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.GET_IMAGE_SIZE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String getImageSize(HttpEntity<String> entity) {
        GetImageSizeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), GetImageSizeCmd.class);
        config.getImageSizeCmds.add(cmd);

        GetImageSizeRsp rsp = new GetImageSizeRsp();
        Long size = config.getImageSizeCmdSize.get(cmd.imageUuid);
        rsp.size = size == null ? 0 : size;
        Long asize = config.getImageSizeCmdActualSize.get(cmd.imageUuid);
        rsp.actualSize = asize == null ? 0 : asize;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.INIT_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String initialize(HttpEntity<String> entity) {
        InitCmd cmd = JSONObjectUtil.toObject(entity.getBody(), InitCmd.class);
        CephBackupStorageConfig cbc = getConfig(cmd);
        config.initCmds.add(cmd);

        DebugUtils.Assert(cbc.fsid != null, String.format("fsid for ceph backup storage[%s] is null", cbc.name));

        InitRsp rsp = new InitRsp();

        if (!config.monInitSuccess) {
            rsp.error = "on purpose";
            rsp.success = false;
        } else {
            rsp.fsid = cbc.fsid;
            rsp.totalCapacity = cbc.totalCapacity;
            rsp.availableCapacity = cbc.availCapacity;
        }

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.DOWNLOAD_IMAGE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String download(HttpEntity<String> entity) {
        DownloadRsp rsp = new DownloadRsp();
        DownloadCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DownloadCmd.class);
        config.downloadCmds.add(cmd);

        Long size = config.imageSize.get(cmd.imageUuid);
        rsp.setSize(size == null ? 0 : size);
        Long asize = config.imageActualSize.get(cmd.imageUuid);
        rsp.setActualSize(asize == null ? 0 : asize);

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=CephBackupStorageBase.DELETE_IMAGE_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String doDelete(HttpEntity<String> entity) {
        DeleteCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteCmd.class);
        config.deleteCmds.add(cmd);
        DeleteRsp rsp = new DeleteRsp();
        reply(entity, rsp);
        return null;
    }
}
