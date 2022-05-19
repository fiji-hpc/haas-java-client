
package cz.it4i.fiji.ssh_hpc_client;

import java.io.Serializable;

import cz.it4i.fiji.hpc_workflow.core.JobType;
import lombok.Data;

@Data
public class NewJobSettings implements Serializable {

	public enum INPUT_OPTION {
			DEMO_DATA, JOB_DIRECTORY, OWN_DIRECTORY
	}

	public enum OUTPUT_OPTION {
			JOB_DIRECTORY, OWN_DIRECTORY
	}

	private int numberOfNodes;

	private int numberOfCoresPerNode;

	private String queueOrPartition;

	private int[] walltime;

	private int maxMemoryPerNode;

	private JobType jobType;

	private INPUT_OPTION inputDataLocationOption;

	private String inputPath;

	private OUTPUT_OPTION outputDataLocationOption;

	private String outputPath;

	private boolean scatter;

}
