/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2019 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.heappe_hpc_client;

import java.util.EnumMap;
import java.util.Map;

import cz.it4i.fiji.haas_java_client.proxy.JobFileContentExt;
import cz.it4i.fiji.haas_java_client.proxy.SynchronizableFilesExt;
import cz.it4i.fiji.hpc_client.JobFileContent;
import cz.it4i.fiji.hpc_client.SynchronizableFileType;
import lombok.AllArgsConstructor;

@AllArgsConstructor
class JobFileContentToExt implements JobFileContent {

	private final JobFileContentExt jobFileContentToExt;

	private static final Map<SynchronizableFileType, SynchronizableFilesExt> type2ext =
		new EnumMap<>(SynchronizableFileType.class);

	private static final Map<SynchronizableFilesExt, SynchronizableFileType> ext2type =
		new EnumMap<>(SynchronizableFilesExt.class);

	static {
		indexType(SynchronizableFileType.LogFile, SynchronizableFilesExt.LOG_FILE);
		indexType(SynchronizableFileType.ProgressFile,
			SynchronizableFilesExt.PROGRESS_FILE);
		indexType(SynchronizableFileType.StandardOutputFile,
			SynchronizableFilesExt.STANDARD_OUTPUT_FILE);
		indexType(SynchronizableFileType.StandardErrorFile,
			SynchronizableFilesExt.STANDARD_ERROR_FILE);
	}

	@Override
	public String getContent() {
		return jobFileContentToExt.getContent();
	}

	@Override
	public long getOffset() {
		return jobFileContentToExt.getOffset();
	}

	@Override
	public SynchronizableFileType getFileType() {
		return ext2type.computeIfAbsent(jobFileContentToExt.getFileType(), type -> {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		});
	}

	@Override
	public String getRelativePath() {
		return jobFileContentToExt.getRelativePath();
	}

	@Override
	public long getTaskId() {
		return jobFileContentToExt.getSubmittedTaskInfoId();
	}

	public static SynchronizableFilesExt getExt(SynchronizableFileType aType) {
		return type2ext.computeIfAbsent(aType, type -> {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		});
	}

	private static void indexType(SynchronizableFileType type,
		SynchronizableFilesExt ext)
	{
		type2ext.put(type, ext);
		ext2type.put(ext, type);
	}

}
