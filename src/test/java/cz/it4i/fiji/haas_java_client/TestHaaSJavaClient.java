package cz.it4i.fiji.haas_java_client;

import static cz.it4i.fiji.haas_java_client.SynchronizableFileRoutines.addOffsetFilesForTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.fiji.hpc_client.HPCClient;
import cz.it4i.fiji.hpc_client.HPCFileTransfer;
import cz.it4i.fiji.hpc_client.JobFileContent;
import cz.it4i.fiji.hpc_client.JobInfo;
import cz.it4i.fiji.hpc_client.JobState;
import cz.it4i.fiji.hpc_client.SynchronizableFile;

public class TestHaaSJavaClient {

	private static Logger log = LoggerFactory.getLogger(cz.it4i.fiji.haas_java_client.TestHaaSJavaClient.class);

	public static void main(String[] args) throws IOException {
		Map<String, String> params = new HashMap<>();
		params.put("inputParam", "someStringParam");
		Path baseDir = Paths.get("/home/koz01/aaa");
		HaaSClient<JobSettings> client = new HaaSClient<>(SettingsProvider
			.getSettings("DD-17-31",
			TestingConstants.CONFIGURATION_FILE_NAME));
		long jobId = client.createJob(new JobSettingsBuilder().jobName("TestOutRedirect").templateId(1l)
			.walltimeLimit(600).clusterNodeType(7l).templateParameters(params
				.entrySet()).build());
		client.submitJob(jobId);
		Path workDir = baseDir.resolve("" + jobId);
		if (!Files.isDirectory(workDir)) {
			Files.createDirectories(workDir);
		}
		JobInfo info;
		do {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			info = client.obtainJobInfo(jobId);
			List<SynchronizableFile> taskFileOffset = new LinkedList<>();
			for (Long id : info.getTasks()) {
				addOffsetFilesForTask(id, taskFileOffset);
			}
			client.downloadPartsOfJobFiles(jobId, taskFileOffset).forEach(
				jfc -> showJFC(jfc));

			if (info.getState() == JobState.Finished) {
				try (HPCFileTransfer fileTransfer = client.startFileTransfer(jobId,
						HPCClient.DUMMY_TRANSFER_FILE_PROGRESS)) {
					for(String file: client.getChangedFiles(jobId)) {
						fileTransfer.download(file, workDir);
					}
					

				}
			}
			log.info("JobId :" + jobId + ", state - " + info.getState());
		} while (info.getState() != JobState.Canceled && info.getState() != JobState.Failed
				&& info.getState() != JobState.Finished);
	}



	private static void showJFC(JobFileContent file) {
		log.info("File: " + file.getFileType() + ", " + file.getRelativePath());
		log.info("TaskInfoId: " + file.getTaskId());
		log.info("Offset: " + file.getOffset());
		log.info("Content: " + file.getContent());
	}

}
