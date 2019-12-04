package cz.it4i.fiji.heappe_hpc_client;

import static cz.it4i.fiji.heappe_hpc_client.LambdaExceptionHandlerWrapper.wrap;
import static cz.it4i.fiji.heappe_hpc_client.SynchronizableFileRoutines.addOffsetFilesForTask;
import static cz.it4i.fiji.hpc_client.Notifiers.emptyTransferFileProgress;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.fiji.heappe_hpc_client.HaaSClient;
import cz.it4i.fiji.heappe_hpc_client.JobSettingsBuilder;
import cz.it4i.fiji.hpc_client.HPCFileTransfer;
import cz.it4i.fiji.hpc_client.JobFileContent;
import cz.it4i.fiji.hpc_client.JobInfo;
import cz.it4i.fiji.hpc_client.JobState;
import cz.it4i.fiji.hpc_client.SynchronizableFile;
import cz.it4i.fiji.hpc_client.UploadingFileImpl;

public class TestHaaSJavaClientWithSPIM {

	private static Logger log = LoggerFactory.getLogger(cz.it4i.fiji.heappe_hpc_client.TestHaaSJavaClientWithSPIM.class);

	public static void main(String[] args) throws IOException {
		HaaSClient client = new HaaSClient(SettingsProvider
			.getSettings("DD-17-31",
			TestingConstants.CONFIGURATION_FILE_NAME));
		Path baseDir = Paths.get("/home/koz01/Work/vyzkumnik/fiji/work/aaa");

		long jobId = client.createJob(new JobSettingsBuilder().jobName("TestOutRedirect").templateId(2)
			.walltimeLimit(9600).clusterNodeType(6).build());

		try (HPCFileTransfer tr = client.startFileTransfer(jobId,
			emptyTransferFileProgress()))
		{
			StreamSupport.stream(getAllFiles(baseDir.resolve("spim-data")).spliterator(), false)
					.map(UploadingFileImpl::new).forEach(f -> wrap(() -> tr.upload(f)));
		}
		client.submitJob(jobId);

		Path workDir = baseDir.resolve("" + jobId);
		if (!Files.isDirectory(workDir)) {
			Files.createDirectories(workDir);
		}
		JobInfo info;
		boolean firstIteration = true;
		do {
			if (!firstIteration) {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			info = client.obtainJobInfo(jobId);
			List<SynchronizableFile> taskFileOffset = new LinkedList<>();
			for (Long id : info.getTasks()) {
				addOffsetFilesForTask(id, taskFileOffset);
			}
			client.downloadPartsOfJobFiles(jobId, taskFileOffset).forEach(jfc -> showJFC(jfc));
			if (info.getState() == JobState.Finished) {
				try (HPCFileTransfer fileTransfer = client.startFileTransfer(jobId,
					emptyTransferFileProgress()))
				{
					client.getChangedFiles(jobId).forEach(file -> wrap(() -> fileTransfer.download(file, workDir)));
				}

			}
			log.info("JobId :" + jobId + ", state" + info.getState());
			firstIteration = false;
		} while (info.getState() != JobState.Canceled && info.getState() != JobState.Failed
				&& info.getState() != JobState.Finished);
	}

	private static Iterable<Path> getAllFiles(Path resolve) {

		return () -> {
			try {
				return Files.newDirectoryStream(resolve).iterator();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		};
	}


	private static void showJFC(JobFileContent file) {
		log.info("File: " + file.getFileType() + ", " + file.getRelativePath());
		log.info("TaskInfoId: " + file.getTaskId());
		log.info("Offset: " + file.getOffset());
		log.info("Content: " + file.getContent());
	}

}
