package org.zstack.header.cluster;

import org.zstack.header.message.Message;

public class ReportHostCapacityMessage extends Message {
    private long totalMemory;
    private long usedCpu;
    private long usedMemory;
    private String hostUuid;
    private int cpuNum;

    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public long getUsedCpu() {
        return usedCpu;
    }

    public void setUsedCpu(long usedCpu) {
        this.usedCpu = usedCpu;
    }

    public long getUsedMemory() {
        return usedMemory;
    }

    public void setUsedMemory(long usedMemory) {
        this.usedMemory = usedMemory;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
