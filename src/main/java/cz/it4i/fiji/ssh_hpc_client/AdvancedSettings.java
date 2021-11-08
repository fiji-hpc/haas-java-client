
package cz.it4i.fiji.ssh_hpc_client;

import cz.it4i.cluster_job_launcher.HPCSchedulerType;
import lombok.Data;

@Data
public class AdvancedSettings {

	private String command; // Path of the ImageJ executable file on remote HPC
													// cluster.
	private String openMpiModule;
	private HPCSchedulerType jobScheduler;

	public AdvancedSettings(String command, String openMpiModule,
		HPCSchedulerType jobScheduler)
	{
		this.command = command;
		this.openMpiModule = openMpiModule;
		this.jobScheduler = jobScheduler;
	}

	public boolean isEmpty() {
		boolean isEmpty = false;
		if (this.command == null && this.openMpiModule == null &&
			this.jobScheduler == null)
		{
			isEmpty = true;
		}
		return isEmpty;
	}

}
