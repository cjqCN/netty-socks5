package com.github.cjqcn.socks5.common.protocol;

public interface ProtocolHandler<T> {

	byte[] encode(T t) throws Throwable;

	T decode(byte[] bytes) throws Throwable;
}
