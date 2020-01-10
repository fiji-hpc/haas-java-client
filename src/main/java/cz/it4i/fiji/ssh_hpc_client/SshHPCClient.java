/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2019 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.ssh_hpc_client;

import static cz.it4i.fiji.hpc_client.JobState.Configuring;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cz.it4i.fiji.heappe_hpc_client.HaaSClientSettings;
import cz.it4i.fiji.hpc_client.HPCClient;
import cz.it4i.fiji.hpc_client.HPCDataTransfer;
import cz.it4i.fiji.hpc_client.HPCFileTransfer;
import cz.it4i.fiji.hpc_client.JobFileContent;
import cz.it4i.fiji.hpc_client.JobInfo;
import cz.it4i.fiji.hpc_client.JobState;
import cz.it4i.fiji.hpc_client.SynchronizableFile;
import cz.it4i.fiji.scpclient.TransferFileProgress;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SshHPCClient implements HPCClient<SSHJobSettings> {

	private Map<Long, JobInfoImpl> states = new HashMap<>();

	private long nextJobId;

	public SshHPCClient(HaaSClientSettings settings) {
		log.info("Creating ssh client with settings " + settings);
	}

	@Override
	public void checkConnection() {

	}

	@Override
	public long createJob(SSHJobSettings jobSettings) {

		long result = ++nextJobId;
		JobInfoImpl js = new JobInfoImpl();
		states.put(result, js);
		return result;
	}

	@Override
	public void submitJob(long jobId) {
		JobInfoImpl jil = states.get(jobId);
		jil.start();
	}

	@Override
	public JobInfo obtainJobInfo(long jobId) {
		return states.computeIfAbsent(jobId, x -> new JobInfoImpl());
	}

	@Override
	public void cancelJob(Long jobId) {
		JobInfoImpl ji = states.get(jobId);
		ji.cancel();
	}

	@Override
	public void deleteJob(long id) {
		JobInfoImpl ji = states.get(id);
		ji.delete();

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
				: Collections
				.emptyList();
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
