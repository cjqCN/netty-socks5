package com.github.cjqcn.socks5.common;

/**
 * @author jqChan
 * @date 2018/6/24
 */
public class DestVisitAddress {

    private String destAddr;

    private int destPort;


    public DestVisitAddress(String destAddr, int destPort) {
        setDestAddr(destAddr);
        setDestPort(destPort);
    }

    public void setDestAddr(String destAddr) {
        this.destAddr = destAddr;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    @Override
    public String toString() {
        return destAddr + ":" + destPort;
    }
}
