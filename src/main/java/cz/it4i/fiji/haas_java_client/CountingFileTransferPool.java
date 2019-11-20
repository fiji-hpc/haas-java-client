/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2019 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.haas_java_client;

import cz.it4i.fiji.haas_java_client.proxy.FileTransferMethodExt;


class CountingFileTransferPool implements FileTransferPool {

	private final FileTransferPool pool;

	private int counter;


	public CountingFileTransferPool(FileTransferPool pool) {
		this.pool = pool;
	}

	@Override
	public synchronized void reconnect() {
		pool.reconnect();
	}

	@Override
	public synchronized FileTransferMethodExt obtain() {

		FileTransferMethodExt result = pool.obtain();
		counter++;
		return result;
	}

	@Override
	public synchronized void release() {
		if (--counter == 0) {
			pool.release();
		}
	}
	
}
