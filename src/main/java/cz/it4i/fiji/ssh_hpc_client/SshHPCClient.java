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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.scijava.plugin.Parameter;

import cz.it4i.cluster_job_launcher.AuthenticationChoice;
import cz.it4i.cluster_job_launcher.ClusterJobLauncher;
import cz.it4i.cluster_job_launcher.HPCSchedulerType;
import cz.it4i.cluster_job_launcher.Job;
import cz.it4i.cluster_job_launcher.JobManager;
import cz.it4i.cluster_job_launcher.JobManagerJobState;
import cz.it4i.cluster_job_launcher.RemoteJobInfo;
import cz.it4i.cluster_job_launcher.SshJobSettings;
import cz.it4i.fiji.heappe_hpc_client.HaaSFileTransferImp;
import cz.it4i.fiji.heappe_hpc_client.paradigm_manager.ui.NewJobController;
import cz.it4i.fiji.hpc_client.HPCClient;
import cz.it4i.fiji.hpc_client.HPCDataTransfer;
import cz.it4i.fiji.hpc_client.HPCFileTransfer;
import cz.it4i.fiji.hpc_client.JobFileContent;
import cz.it4i.fiji.hpc_client.JobFileContentSsh;
import cz.it4i.fiji.hpc_client.JobInfo;
import cz.it4i.fiji.hpc_client.JobState;
import cz.it4i.fiji.hpc_client.SynchronizableFile;
import cz.it4i.fiji.hpc_client.SynchronizableFileType;
import cz.it4i.fiji.hpc_workflow.core.Constants;
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

		// The HPC Scheduler type will be automatically detected (if set to null):
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
			this.scpClient.setPort(settings.getPort());

			remoteWorkingDirectory = settings.getRemoteWorkingDirectory();

			remoteFijiDirectory = settings.getRemoteDirectory();

			command = settings.getCommand();

			// Create the remote working directory if it does not exist:
			this.cjlClient.createRemoteDirectory(remoteWorkingDirectory);

			// ToDo: find a way to provide the scheduler to NewJobController without a
			// static variable:
			NewJobController.hpcSchedulerType = this.cjlClient.getSchedulerType();
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
		String jobRemotePath = this.remoteWorkingDirectory +
			Constants.FORWARD_SLASH + workflowJobId + Constants.FORWARD_SLASH;
		this.cjlClient.createRemoteDirectory(jobRemotePath);
		// Create workflow job info, this is were the number of nodes and cores per
		// node are stored:
		this.cjlClient.setRemoteJobInfo(jobRemotePath, jobSettings);

		return workflowJobId;
	}

	@Override
	public void submitJob(long jobId) {
		String jobRemotePath = this.remoteWorkingDirectory +
			Constants.FORWARD_SLASH + jobId + Constants.FORWARD_SLASH;

		// Get the info of the workflow job from the remote cluster:
		RemoteJobInfo jobRemoteInfo = cjlClient.getRemoteJobInfo(jobRemotePath);

		List<String> modules = new ArrayList<>();
		modules.add("openmpi/4");

		String parameters;
		String jobRemotePathWithScript;
		String userScriptName = jobRemoteInfo.getUserScriptName();
		if (userScriptName.contains(".ijm")) {
			parameters = " --headless --console -macro ";
			jobRemotePathWithScript = jobRemotePath + SCRIPT_FILE;
		}
		else if (userScriptName.contains("py")) {
			parameters = " --ij2 --headless --console --run ";
			jobRemotePathWithScript = jobRemotePath + userScriptName;
		}
		else {
			SimpleDialog.showWarning("Unsupported",
				"SSH is not compatible with this type of job. Please use the middoleware.");
			return;
		}

		Job job = this.cjlClient.submitOpenMpiJob(this.remoteFijiDirectory,
			this.command, parameters + " " + jobRemotePathWithScript, jobRemoteInfo
				.getNumberOfNodes(), jobRemoteInfo.getNumberOfCoresPerNode(), modules,
			jobRemotePath, jobRemoteInfo.getSlurmPartitionOrPbsQueueType());

		this.cjlClient.setJobIdInfo(jobRemotePath, job.getID());
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
		this.cjlClient.removeRemoteDirectory(this.remoteWorkingDirectory +
			Constants.FORWARD_SLASH + jobId);
		log.info("Remove remote job directory: " + this.remoteWorkingDirectory +
			Constants.FORWARD_SLASH + jobId);
	}

	@Override
	public HPCFileTransfer startFileTransfer(long jobId,
		TransferFileProgress notifier)
	{
		return new HaaSFileTransferImp(remoteWorkingDirectory +
			Constants.FORWARD_SLASH + jobId, this.scpClient, notifier);
	}

	@Parameter
	private RedirectingOutputService redirectingOutputService;

	private RedirectedOutputService redirectedOutput = null;

	private Map<String, String> outputTextBySchedulerJobId = new HashMap<>();
	private Map<String, String> errorTextBySchedulerJobId = new HashMap<>();
	private Map<Long, String> jobIdToSchedulerJobId = new HashMap<>();

	private Map<Long, Job> jobsByJobId = new HashMap<>();

	private Map<Long, Long> timeLastPolledByJobId = new HashMap<>();
	private static final long TIMEOUT = 10000L;
	private Map<Long, Timer> timersByJobId = new HashMap<>();

	private Object lock = new Object();

	private void createPeriodicTaskToCheckIfOutputRedirectionMustStop(
		long jobId)
	{
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				synchronized (lock) {
					long now = Instant.now().toEpochMilli();
					if (now - timeLastPolledByJobId.get(jobId) > TIMEOUT) {
						log.debug("Job {} redirected output has timed-out.", jobId);
						// Stop the bus.
						redirectedOutput.post(new FeedbackMessage(false,
							jobIdToSchedulerJobId.get(jobId)));
						timersByJobId.get(jobId).cancel();
						timersByJobId.remove(jobId);

						// Remove standard and error output for this job:
						String schedulerJobId = jobIdToSchedulerJobId.get(jobId);
						outputTextBySchedulerJobId.remove(schedulerJobId);
						errorTextBySchedulerJobId.remove(schedulerJobId);
						// Remove job from lists:
						jobIdToSchedulerJobId.remove(jobId);
						timeLastPolledByJobId.remove(jobId);

					}
				}
			}
		};
		timersByJobId.putIfAbsent(jobId, new Timer("Timer"));

		long period = 1000L;
		timersByJobId.get(jobId).scheduleAtFixedRate(task, 0, period);
	}

	@Override
	public List<JobFileContent> downloadPartsOfJobFiles(Long jobId,
		List<SynchronizableFile> files)
	{
		synchronized (lock) {
			this.timeLastPolledByJobId.put(jobId, Instant.now().toEpochMilli());
			List<JobFileContent> results = new ArrayList<>();

			if (!this.jobIdToSchedulerJobId.containsKey(jobId)) {
				// Get the redirecting output service of the job:
				JobManager jobManager = this.cjlClient.getJobManager(
					remoteWorkingDirectory, jobId);

				// Only start if the job is running or has run:
				JobManagerJobState jobState = jobManager.getState();
				if (jobState == JobManagerJobState.CONFIGURING ||
					jobState == JobManagerJobState.QUEUED)
				{
					return results;
				}

				String schedulerJobId = jobManager.getSchedulerJobId();

				// If there is no scheduler id the job has never been started before and
				// there is no output to redirect yet.
				if (schedulerJobId.equals("none")) {
					log.debug("The job has never run before and it has no output.");
					// Return the empty list.
					return results;
				}

				this.jobIdToSchedulerJobId.put(jobId, schedulerJobId);

				// If the same job dashboard is open more than once it should not make
				// the output and error blank. Create the initial empty output and error
				// strings if they do not exist:
				this.outputTextBySchedulerJobId.putIfAbsent(schedulerJobId, "");
				this.errorTextBySchedulerJobId.putIfAbsent(schedulerJobId, "");

				// Register for the messages on the bus:
				if (!jobsByJobId.containsKey(jobId)) {
					cjlClient.startOutputRedirectionServiceIfNotStarted();
					jobsByJobId.put(jobId, this.cjlClient.getSubmittedJob(
						schedulerJobId));
					jobsByJobId.get(jobId).startPublishing();
				}
				Job job = jobsByJobId.get(jobId);
				if (redirectedOutput == null) {
					redirectedOutput = (RedirectedOutputService) job
						.getOutputRedirectionService();
					redirectedOutput.register(this);
				}
				redirectedOutput.post(new FeedbackMessage(true, schedulerJobId));

				// Create a task to check if there is timeout:
				createPeriodicTaskToCheckIfOutputRedirectionMustStop(jobId);
			}

			// Find the order in which the file types should be placed in the list:
			int outputFileIndex = 1;
			int errorFileIndex = 0;
			if (files.get(0).getType() == SynchronizableFileType.StandardOutputFile) {
				outputFileIndex = 0;
				errorFileIndex = 1;
			}

			// Find the new text:
			String schedulerJobId = this.jobIdToSchedulerJobId.get(jobId);

			JobFileContent outputResult = getNewTextAsSynchronizableFile(files,
				outputFileIndex, schedulerJobId, jobId, outputTextBySchedulerJobId,
				SynchronizableFileType.StandardOutputFile);

			JobFileContent errorResult = getNewTextAsSynchronizableFile(files,
				errorFileIndex, schedulerJobId, jobId, errorTextBySchedulerJobId,
				SynchronizableFileType.StandardErrorFile);

			// Place the files in the correct order:
			for (int i = 0; i <= 1; i++) {
				if (i == outputFileIndex) {
					results.add(outputResult);
				}
				else {
					results.add(errorResult);
				}
			}

			return results;
		}
	}

	private JobFileContentSsh getNewTextAsSynchronizableFile(
		List<SynchronizableFile> files, int fileIndex, String schedulerJobId,
		Long jobId, Map<String, String> textBySchedulerJobId,
		SynchronizableFileType fileType)
	{
		int offset = (int) files.get(fileIndex).getOffset();
		String newText = "";
		String existingText = textBySchedulerJobId.get(schedulerJobId);
		if (existingText.length() > offset && !existingText.isEmpty()) {
			newText = existingText.substring(offset);
		}
		return new JobFileContentSsh(newText, "/", jobId, offset, fileType);
	}

	@Subscribe
	public void handleEvent(final EventMessage eventMessage) {
		if (eventMessage.getMsgcode() == OutputType.OUTPUT) {
			String oldOutput = this.outputTextBySchedulerJobId.get(eventMessage
				.getJobId());
			String newOutput = oldOutput + eventMessage.getMsg();
			this.outputTextBySchedulerJobId.put(eventMessage.getJobId(), newOutput);
		}
		else {
			String oldError = this.errorTextBySchedulerJobId.get(eventMessage
				.getJobId());
			String newError = oldError + eventMessage.getMsg();
			this.errorTextBySchedulerJobId.put(eventMessage.getJobId(), newError);
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

	@Override
	public void close() {
		this.scpClient.close();
		this.cjlClient.close();
	}
}
