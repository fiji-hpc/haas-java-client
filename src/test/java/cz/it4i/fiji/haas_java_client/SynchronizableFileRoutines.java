/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2019 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.haas_java_client;

import java.util.List;

import cz.it4i.fiji.hpc_client.SynchronizableFile;
import cz.it4i.fiji.hpc_client.SynchronizableFileType;

final class SynchronizableFileRoutines {

	private SynchronizableFileRoutines() {}

	static void addOffsetFilesForTask(Long taskId,
		List<SynchronizableFile> files)
	{
		for (SynchronizableFileType type : SynchronizableFileType.values()) {
			files.add(new SynchronizableFile(taskId, type, 0));
		}
	}
}
