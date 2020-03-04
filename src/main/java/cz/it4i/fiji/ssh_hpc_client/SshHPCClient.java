/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2019 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.ssh_hpc_client;

import com.google.common.eventbus.Subscribe;
import com.jcraft.jsch.JSchException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scijava.plugin.Parameter;

import cz.it4i.cluster_job_launcher.ClusterJobLauncher;
import cz.it4i.cluster_job_launcher.ClusterJobLauncher.Job;
import cz.it4i.cluster_job_launcher.HPCSchedulerType;
import cz.it4i.cluster_job_launcher.JobManager;
import cz.it4i.cluster_job_launcher.JobManagerJobState;
import cz.it4i.fiji.heappe_hpc_client.HaaSFileTransferImp;
import cz.it4i.fiji.hpc_client.HPCClient;
import cz.it4i.fiji.hpc_client.HPCDataTransfer;
import cz.it4i.fiji.hpc_client.HPCFileTransfer;
import cz.it4i.fiji.hpc_client.JobFileContent;
import cz.it4i.fiji.hpc_client.JobFileContentSsh;
import cz.it4i.fiji.hpc_client.JobInfo;
import cz.it4i.fiji.hpc_client.JobState;
import cz.it4i.fiji.hpc_client.SynchronizableFile;
import cz.it4i.fiji.hpc_client.SynchronizableFileType;
import cz.it4i.fiji.scpclient.ScpClient;
import cz.it4i.fiji.scpclient.TransferFileProgress;
import cz.it4i.parallel.runners.logging.ui.EventMessage;
import cz.it4i.parallel.runners.logging.ui.FeedbackMessage;
import cz.it4i.parallel.runners.logging.ui.RedirectedOutputService;
import cz.it4i.parallel.runners.logging.ui.RedirectingOutputService;
import cz.it4i.parallel.runners.logging.ui.RedirectingOutputService.OutputType;
import cz.it4i.swing_javafx_ui.JavaFXRoutines;
import cz.it4i.swing_javafx_ui.SimpleDialog;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SshHPCClient implements HPCClient<SshJobSettings> {

	private ClusterJobLauncher cjlClient;

	private String remoteWorkingDirectory;

	private ScpClient scpClient;

	private String command;

	private String remoteFijiDirectory;

	private static final String SCRIPT_FILE = "parallelMacroWrappedScript.ijm";

	public SshHPCClient(SshConnectionSettings settings) {
		log.info("Creating ssh client with given settings.");

		// The HPC Scheduler type will be automatically detected:
		HPCSchedulerType schedulerType = null;

		try {
			if (settings.getAuthenticationChoice() == AuthenticationChoice.KEY_FILE) {
				this.cjlClient = ClusterJobLauncher.createWithKeyAuthentication(settings
					.getHost(), settings.getPort(), settings.getUserName(), settings
						.getKeyFile().getAbsolutePath(), settings.getKeyFilePassword(),
					schedulerType, true);

				this.scpClient = new ScpClient(settings.getHost(), settings
					.getUserName(), settings.getKeyFile().getAbsolutePath(), settings
						.getKeyFilePassword());
			}
			else {
				this.cjlClient = ClusterJobLauncher.createWithPasswordAuthentication(
					settings.getHost(), settings.getPort(), settings.getUserName(),
					settings.getPassword(), schedulerType, true);

				this.scpClient = new ScpClient(settings.getHost(), settings
					.getUserName(), settings.getPassword());
			}

			remoteWorkingDirectory = settings.getRemoteWorkingDirectory();

			remoteFijiDirectory = settings.getRemoteDirectory();

			command = settings.getCommand();

			// Create the remote working directory if it does not exist:
			this.cjlClient.createRemoteDirectory(remoteWorkingDirectory);
		}
		catch (JSchException exc) {
			JavaFXRoutines.runOnFxThread(() -> SimpleDialog.showException("Exception",
				"Could not connect to HPC Cluster using SSH.", exc));
		}
	}

	@Override
	public void checkConnection() {
		// This does nothing.
	}

	@Override
	public long createJob(SshJobSettings jobSettings) {
		long workflowJobId = this.cjlClient.getLastJobIdFromRemoteWorkingDirectory(
			this.remoteWorkingDirectory) + 1;
		// Create job directory on remote working directory as well:
		log.info("Create remote job directory: " + this.remoteWorkingDirectory +
			"/" + workflowJobId);
		String jobRemotePath = this.remoteWorkingDirectory + "/" + workflowJobId +
			"/";
		this.cjlClient.createRemoteDirectory(jobRemotePath);
		// Create workflow job info, this is were the number of nodes and cores per
		// node are stored:
		this.cjlClient.storeTextInRemoteFile(jobRemotePath, jobSettings
			.getNumberOfNodes() + "\n" + jobSettings.getNumberOfCoresPerNode(),
			JobManager.WORKFLOW_JOB_INFO);

		return workflowJobId;
	}

	@Override
	public void submitJob(long jobId) {
		String jobRemotePath = this.remoteWorkingDirectory + "/" + jobId + "/";
		String jobRemotePathWithScript = jobRemotePath + SCRIPT_FILE;

		String parameters = " --headless --console -macro ";

		// Get the info of the workflow job from the remote cluster:
		List<String> remoteWorkflowJobInfo = this.cjlClient.readTextFromRemoteFile(
			jobRemotePath, JobManager.WORKFLOW_JOB_INFO);
		long numberOfNodes = Long.parseLong(remoteWorkflowJobInfo.get(0));
		long numberOfCoresPerNode = Long.parseLong(remoteWorkflowJobInfo.get(1));

		List<String> modules = new ArrayList<>();
		modules.add("OpenMPI/4.0.0-GCC-6.3.0-2.27");
		modules.add("list");

		Job job = this.cjlClient.submitOpenMpiJob(this.remoteFijiDirectory,
			this.command, parameters + " " + jobRemotePathWithScript, numberOfNodes,
			numberOfCoresPerNode, modules, jobRemotePath);

		this.cjlClient.storeTextInRemoteFile(jobRemotePath, job.getID(),
			JobManager.JOB_ID_FILE);
	}

	@Override
	public JobInfo obtainJobInfo(long jobId) {
		return new JobInfoImpl(jobId);
	}

	@Override
	public void cancelJob(Long jobId) {
		JobManager jobManager = getJobManager(this.remoteWorkingDirectory, jobId);
		jobManager.cancel();
	}

	@Override
	public void deleteJob(long jobId) {
		this.cjlClient.removeRemoteDirectory(this.remoteWorkingDirectory + "/" +
			jobId);
		log.info("Remove remote job directory: " + this.remoteWorkingDirectory +
			"/" + jobId);
	}

	@Override
	public HPCFileTransfer startFileTransfer(long jobId,
		TransferFileProgress notifier)
	{
		return new HaaSFileTransferImp(remoteWorkingDirectory + "/" + jobId,
			this.scpClient, notifier);
	}

	@Parameter
	private RedirectingOutputService redirectingOutputService;

	private RedirectedOutputService redirectedOutput = null;

	private Map<String, String> outputText = new HashMap<>();
	private Map<String, String> errorText = new HashMap<>();
	private Map<String, Job> outputRedirectingJobs = new HashMap<>();
	private Map<Long, String> jobIdToSchedulerJobIdMap = new HashMap<>();

	@Override
	public List<JobFileContent> downloadPartsOfJobFiles(Long jobId,
		List<SynchronizableFile> files)
	{
		if (!this.jobIdToSchedulerJobIdMap.containsKey(jobId)) {
			// Get the redirecting output service of the job:
			JobManager jobManager = this.cjlClient.getJobManager(
				remoteWorkingDirectory, jobId);
			String schedulerJobId = jobManager.getSchedulerJobId();
			Job job = this.cjlClient.getSubmittedJob(schedulerJobId);
			this.outputRedirectingJobs.put(schedulerJobId, job);
			this.jobIdToSchedulerJobIdMap.put(jobId, schedulerJobId);

			// Create the initial empty error and output strings:
			this.outputText.put(schedulerJobId, "");
			this.errorText.put(schedulerJobId, "");
		}

		if (redirectedOutput == null) {
			String schedulerJobId = this.jobIdToSchedulerJobIdMap.get(jobId);
			redirectedOutput = (RedirectedOutputService) this.outputRedirectingJobs
				.get(schedulerJobId).getOutputRedirectionService();
			redirectedOutput.register(this);
			redirectedOutput.post(new FeedbackMessage(true));
		}

		int outputOffset = (int) files.get(0).getOffset();
		String schedulerJobId = this.jobIdToSchedulerJobIdMap.get(jobId);
		String outputTextNew = this.outputText.get(schedulerJobId).substring(
			outputOffset);
		JobFileContent outputResult = new JobFileContentSsh(outputTextNew, "/",
			jobId, outputOffset, SynchronizableFileType.StandardOutputFile);

		int errorOffset = (int) files.get(1).getOffset();
		String errorTextNew = this.errorText.get(schedulerJobId).substring(
			errorOffset);
		JobFileContent errorResult = new JobFileContentSsh(errorTextNew, "/", jobId,
			errorOffset, SynchronizableFileType.StandardErrorFile);

		List<JobFileContent> results = new ArrayList<>();
		results.add(outputResult);
		results.add(errorResult);

		return results;
	}

	@Subscribe
	public void handleEvent(final EventMessage eventMessage) {
		if (eventMessage.getMsgcode() == OutputType.OUTPUT) {
			String oldOutput = this.outputText.get(eventMessage.getJobId());
			String newOutput = oldOutput + eventMessage.getMsg();
			this.outputText.put(eventMessage.getJobId(), newOutput);
		}
		else {
			String oldError = this.errorText.get(eventMessage.getJobId());
			String newError = oldError + eventMessage.getMsg();
			this.errorText.put(eventMessage.getJobId(), newError);
		}
	}

	@Override
	public Collection<String> getChangedFiles(long jobId) {
		return ((JobInfoImpl) obtainJobInfo(jobId)).getChangedFiles();
	}

	@Override
	public HPCDataTransfer startDataTransfer(long jobId, int nodeNumber,
		int port)
	{
		// TODO Auto-generated method stub
		return null;
	}

	private final class JobInfoImpl implements JobInfo {

		private long workflowJobId;
		private JobManager jobManager;

		JobInfoImpl(long newJobId) {
			this.workflowJobId = newJobId;
			this.jobManager = getJobManager(remoteWorkingDirectory, workflowJobId);
		}

		@Override
		public Collection<Long> getTasks() {
			return Collections.singleton(this.workflowJobId);
		}

		@Override
		public JobState getState() {
			return convertJobState(jobManager.getState());
		}

		private JobState convertJobState(JobManagerJobState jobManagerState) {
			Map<JobManagerJobState, JobState> jobStates = new EnumMap<>(
				JobManagerJobState.class);
			jobStates.put(JobManagerJobState.UNKNOWN, JobState.Unknown);
			jobStates.put(JobManagerJobState.CONFIGURING, JobState.Configuring);
			jobStates.put(JobManagerJobState.SUBMITTED, JobState.Submitted);
			jobStates.put(JobManagerJobState.QUEUED, JobState.Queued);
			jobStates.put(JobManagerJobState.RUNNING, JobState.Running);
			jobStates.put(JobManagerJobState.FINISHED, JobState.Finished);
			jobStates.put(JobManagerJobState.FAILED, JobState.Failed);
			jobStates.put(JobManagerJobState.CANCELED, JobState.Canceled);
			jobStates.put(JobManagerJobState.DISPOSED, JobState.Disposed);
			return jobStates.get(jobManagerState);
		}

		@Override
		public Calendar getStartTime() {
			return jobManager.getStartTime();
		}

		@Override
		public Calendar getEndTime() {
			return jobManager.getEndTime();
		}

		@Override
		public Calendar getCreationTime() {
			return jobManager.getCreationTime();
		}

		@Override
		public List<String> getNodesIPs() {
			return getState() == JobState.Running ? getNodes(jobManager
				.getSchedulerJobId()) : Collections.emptyList();
		}

		public Collection<String> getChangedFiles() {
			return jobManager.getChangedFiles();
		}
	}

	private List<String> getNodes(String jobId) {
		return this.cjlClient.getSubmittedJob(jobId).getNodes();
	}

	private JobManager getJobManager(String newRemoteWorkingDirectory,
		long newWorkflowJobId)
	{
		return this.cjlClient.getJobManager(newRemoteWorkingDirectory,
			newWorkflowJobId);
	}
}
