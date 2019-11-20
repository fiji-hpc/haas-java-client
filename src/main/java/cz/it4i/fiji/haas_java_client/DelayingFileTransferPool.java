/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2019 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.haas_java_client;

import java.util.Timer;
import java.util.TimerTask;

import cz.it4i.fiji.haas_java_client.proxy.FileTransferMethodExt;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class DelayingFileTransferPool implements FileTransferPool {

	private static final int TIMEOUT_MILISECONDS = 30000;

	private final FileTransferPool pool;

	private TimerTask releasingTask;

	private Timer timer = new Timer();
	@Override
	public void reconnect() {
		pool.reconnect();
	}

	@Override
	public synchronized FileTransferMethodExt obtain() {
		cancelReleaseTask();
		return pool.obtain();
	}

	@Override
	public synchronized void release() {
		cancelReleaseTask();
		releasingTask = new PReleasingTask();
		timer.schedule(releasingTask, TIMEOUT_MILISECONDS);
	}

	private void cancelReleaseTask() {
		if (releasingTask != null) {
			releasingTask.cancel();
			releasingTask = null;
		}
	}

	private class PReleasingTask extends TimerTask {

		private final Thread shutDownHook;

		public PReleasingTask() {
			shutDownHook = new Thread() {
				@Override
				public void run() {
					synchronized (DelayingFileTransferPool.this) {
						doRelease();
					}
				}
			};
			Runtime.getRuntime().addShutdownHook(shutDownHook);
		}

		@Override
		public boolean cancel() {
			Runtime.getRuntime().removeShutdownHook(shutDownHook);
			return super.cancel();
		}

		@Override
		public void run() {
			synchronized (DelayingFileTransferPool.this) {
				Runtime.getRuntime().removeShutdownHook(shutDownHook);
				doRelease();
			}
		}

		private void doRelease() {
			if (releasingTask != null) {
				pool.release();
				releasingTask = null;
			}
		}
	}
}
