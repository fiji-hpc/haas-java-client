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

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cz.it4i.cluster_job_launcher.ClusterJobLauncher;
import cz.it4i.cluster_job_launcher.HPCSchedulerType;
import cz.it4i.fiji.hpc_client.HPCClient;
import cz.it4i.fiji.hpc_client.HPCDataTransfer;
import cz.it4i.fiji.hpc_client.HPCFileTransfer;
import cz.it4i.fiji.hpc_client.JobFileContent;
import cz.it4i.fiji.hpc_client.JobInfo;
import cz.it4i.fiji.hpc_client.JobState;
import cz.it4i.fiji.hpc_client.SynchronizableFile;
import cz.it4i.fiji.scpclient.TransferFileProgress;
import cz.it4i.swing_javafx_ui.JavaFXRoutines;
import cz.it4i.swing_javafx_ui.SimpleDialog;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SshHPCClient implements HPCClient<SshJobSettings> {

	private Map<Long, JobInfoImpl> states = new HashMap<>();

	private long nextWorkflowJobId;

	private ClusterJobLauncher client;

	public SshHPCClient(SshConnectionSettings settings) {
		log.info("Creating ssh client with given settings.");

		HPCSchedulerType schedulerType;
		if (settings.getSchedulerType().equals("PBS")) {
			schedulerType = HPCSchedulerType.PBS;
		}
		else {
			schedulerType = HPCSchedulerType.SLURM;
		}

		try {
			if (settings.getAuthenticationChoice() == AuthenticationChoice.KEY_FILE) {
				this.client = ClusterJobLauncher.createWithKeyAuthentication(settings
					.getHost(), settings.getPort(), settings.getUserName(), settings
						.getKeyFile().getAbsolutePath(), settings.getKeyFilePassword(),
					schedulerType, true);
			}
			else {
				this.client = ClusterJobLauncher.createWithPasswordAuthentication(
					settings.getHost(), settings.getPort(), settings.getUserName(),
					settings.getPassword(), schedulerType, true);
			}

			// Create the remote working directory if it does not exist:
			this.client.createRemoteDirectory(settings.getRemoteWorkingDirectory());
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
		long workflowJobId = ++nextWorkflowJobId;
		JobInfoImpl jobInfoImpl = new JobInfoImpl();
		states.put(workflowJobId, jobInfoImpl);
		return workflowJobId;
	}

	@Override
	public void submitJob(long jobId) {
		JobInfoImpl jobInfoImpl = states.get(jobId);
		jobInfoImpl.start();
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

	}

	@Override
	public HPCFileTransfer startFileTransfer(long jobId,
		TransferFileProgress notifier)
	{
		return new HPCFileTransferAdapter();
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
