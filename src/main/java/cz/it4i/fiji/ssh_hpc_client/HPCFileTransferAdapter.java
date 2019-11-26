/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2019 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.ssh_hpc_client;

import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import cz.it4i.fiji.hpc_client.HPCFileTransfer;
import cz.it4i.fiji.hpc_client.UploadingFile;
import cz.it4i.fiji.scpclient.TransferFileProgress;


class HPCFileTransferAdapter implements HPCFileTransfer {

	@Override
	public void close() {
	}

	@Override
	public void upload(UploadingFile file) throws InterruptedIOException {
	}

	@Override
	public void download(String files, Path workDirectory)
		throws InterruptedIOException
	{
	}

	@Override
	public List<Long> obtainSize(List<String> files)
		throws InterruptedIOException
	{
		return files.stream().map(g -> 0l).collect(Collectors.toList());
	}

	@Override
	public List<String> getContent(List<String> logs) {
		return logs.stream().map(g -> "").collect(Collectors.toList());
	}

	@Override
	public void setProgress(TransferFileProgress progress) {

	}

}
