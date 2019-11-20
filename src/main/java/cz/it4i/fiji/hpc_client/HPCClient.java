/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2019 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.hpc_client;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import cz.it4i.fiji.haas_java_client.TunnelToNode;
import cz.it4i.fiji.scpclient.TransferFileProgress;

public interface HPCClient<T> {

	TransferFileProgress DUMMY_TRANSFER_FILE_PROGRESS = bytesTransfered -> {};
	ProgressNotifier DUMMY_PROGRESS_NOTIFIER = new ProgressNotifier() {

		@Override
		public void setTitle(final String title) {
			// NOP
		}

		@Override
		public void setItemCount(final int count, final int total) {
			// NOP
		}

		@Override
		public void setCount(final int count, final int total) {
			// NOP
		}

		@Override
		public void itemDone(final Object item) {
			// NOP
		}

		@Override
		public void done() {
			// NOP
		}

		@Override
		public void addItem(final Object item) {
			// NOP
		}
	};

	void checkConnection();

	long createJob(T jobSettings,
		Collection<Entry<String, String>> templateParameters);

	HPCFileTransfer startFileTransfer(long jobId, TransferFileProgress notifier);

	HPCFileTransfer startFileTransfer(long jobId);

	TunnelToNode openTunnel(long jobId, String nodeIP, int localPort,
		int remotePort);

	void submitJob(long jobId);

	JobInfo obtainJobInfo(long jobId);

	List<JobFileContent> downloadPartsOfJobFiles(Long jobId,
		List<SynchronizableFile> files);

	Collection<String> getChangedFiles(long jobId);

	void cancelJob(Long jobId);

	void deleteJob(long id);

	HPCDataTransfer startDataTransfer(long jobId, int nodeNumber, int port);

}
