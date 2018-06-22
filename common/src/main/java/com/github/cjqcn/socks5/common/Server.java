package com.github.cjqcn.socks5.common;

public interface Server {

	/**
	 * Starts the server
	 *
	 * @throws Throwable
	 */
	void start() throws Throwable;

	/**
	 * Stops the server
	 *
	 * @throws Throwable
	 */
	void stop() throws Throwable;

}
