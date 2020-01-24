/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2019 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/

package cz.it4i.fiji.ssh_hpc_client;

import static cz.it4i.fiji.hpc_client.JobState.Configuring;

import com.jcraft.jsch.JSchException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cz.it4i.cluster_job_launcher.ClusterJobLauncher;
import cz.it4i.cluster_job_launcher.HPCSchedulerType;
import cz.it4i.fiji.heappe_hpc_client.HaaSFileTransferImp;
import cz.it4i.fiji.hpc_client.HPCClient;
import cz.it4i.fiji.hpc_client.HPCDataTransfer;
import cz.it4i.fiji.hpc_client.HPCFileTransfer;
import cz.it4i.fiji.hpc_client.JobFileContent;
import cz.it4i.fiji.hpc_client.JobInfo;
import cz.it4i.fiji.hpc_client.JobState;
import cz.it4i.fiji.hpc_client.SynchronizableFile;
import cz.it4i.fiji.scpclient.ScpClient;
import cz.it4i.fiji.scpclient.TransferFileProgress;
import cz.it4i.swing_javafx_ui.JavaFXRoutines;
import cz.it4i.swing_javafx_ui.SimpleDialog;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SshHPCClient implements HPCClient<SshJobSettings> {

	// Maps job id with the JobInfoImpl:
	private Map<Long, JobInfoImpl> states = new HashMap<>();

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
		JobInfoImpl jobInfoImpl = new JobInfoImpl();
		states.put(workflowJobId, jobInfoImpl);
		// Create job directory on remote working directory as well:
		this.cjlClient.createRemoteDirectory(this.remoteWorkingDirectory + "/" +
			workflowJobId);
		log.info("Create remote job directory: " + this.remoteWorkingDirectory +
			"/" + workflowJobId);
		return workflowJobId;
	}

	@Override
	public void submitJob(long jobId) {
		JobInfoImpl jobInfoImpl = states.get(jobId);
		jobInfoImpl.start();
		String jobRemotePath = this.remoteWorkingDirectory + "/" + jobId + "/" +
			SCRIPT_FILE;

		String parameters = " --headless --console -macro ";

		// ToDo: use user specified number as given during creation of the job.
		long numberOfNodes = 2;
		long numberOfCoresPerNode = 24;

		List<String> modules = new ArrayList<>();
		modules.add("OpenMPI/4.0.0-GCC-6.3.0-2.27");
		modules.add("list");
		// modules.add("OpenMPI");

		this.cjlClient.submitOpenMpiJob(this.remoteFijiDirectory, this.command,
			parameters + " " + jobRemotePath, numberOfNodes, numberOfCoresPerNode,
			modules);
	}

	@Override
	public JobInfo obtainJobInfo(long jobId) {
		return states.computeIfAbsent(jobId, x -> new JobInfoImpl());
	}

	@Override
	public void cancelJob(Long jobId) {
		JobInfoImpl jobInfoImpl = states.get(jobId);
		jobInfoImpl.cancel();
	}

	@Override
	public void deleteJob(long id) {
		JobInfoImpl jobInfoImpl = states.get(id);
		jobInfoImpl.delete();
		this.cjlClient.removeRemoteDirectory(this.remoteWorkingDirectory + "/" +
			id);
		log.info("Remove remote job directory: " + this.remoteWorkingDirectory +
			"/" + id);
	}

	@Override
	public HPCFileTransfer startFileTransfer(long jobId,
		TransferFileProgress notifier)
	{
		return new HaaSFileTransferImp(remoteWorkingDirectory + "/" + jobId,
			this.scpClient, notifier);
	}

	@Override
	public List<JobFileContent> downloadPartsOfJobFiles(Long jobId,
		List<SynchronizableFile> files)
	{
		return null;
	}

	@Override
	public Collection<String> getChangedFiles(long jobId) {
		return Collections.emptyList();
	}

	@Override
	public HPCDataTransfer startDataTransfer(long jobId, int nodeNumber,
		int port)
	{
		// TODO Auto-generated method stub
		return null;
	}

	private static final class JobInfoImpl implements JobInfo {

		private JobState state;

		private Calendar startTime;

		private Calendar endTime;

		private final Calendar creationTime;

		JobInfoImpl() {
			creationTime = Calendar.getInstance();
		}

		@Override
		public Collection<Long> getTasks() {
			return getState() == JobState.Running ? Collections.singleton(1l)
				: Collections.emptyList();
		}

		@Override
		public JobState getState() {
			if (state != null) {
				return state;
			}
			if (startTime == null) {
				return Configuring;
			}
			Calendar now = Calendar.getInstance();
			if (now.before(endTime)) {
				return JobState.Running;
			}
			return JobState.Finished;
		}

		@Override
		public Calendar getStartTime() {
			return startTime;
		}

		@Override
		public Calendar getEndTime() {
			return endTime;
		}

		@Override
		public Calendar getCreationTime() {
			return creationTime;
		}

		@Override
		public List<String> getNodesIPs() {
			return getState() == JobState.Running ? Collections.singletonList(
				"127.0.0.1") : Collections.emptyList();
		}

		void start() {
			startTime = Calendar.getInstance();
			endTime = Calendar.getInstance();
			endTime.add(Calendar.MILLISECOND, (new Random().nextInt(20) + 30) * 1000);

		}

		void cancel() {
			state = JobState.Canceled;
		}

		void delete() {
			state = JobState.Disposed;
		}

	}
}
