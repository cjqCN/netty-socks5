package com.github.cjqcn.socks5.common.encrypt;

public class DataHandler implements Encode, Decode {

    static byte[] decodeDir = new byte[256];
    static byte[] encodeDir = new byte[256];


    public byte[] decode(byte[] ripe) throws Exception {
        return new byte[0];
    }

    public byte[] encode(byte[] raw) throws Exception {
        return new byte[0];
    }


    public static byte encodeByte(byte raw) {
        return encodeDir[raw + 128];
    }


    public static byte decodeByte(byte ripe) {
        return decodeDir[ripe + 128];
    }


    public static void main(String[] args){

    }

}
