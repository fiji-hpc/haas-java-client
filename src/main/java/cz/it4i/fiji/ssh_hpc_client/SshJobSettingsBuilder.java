
package cz.it4i.fiji.ssh_hpc_client;

import cz.it4i.cluster_job_launcher.SshJobSettings;

public class SshJobSettingsBuilder {

	private static final int DEFAULT_NUMBER_OF_NODES = 1;
	private static final int DEFAULT_NUMBER_OF_CORES_PER_NODE = 24;

	private int numberOfNodes = DEFAULT_NUMBER_OF_NODES;
	private int numberOfCoresPerNode = DEFAULT_NUMBER_OF_CORES_PER_NODE;

	private String queueOrPartition;

	public SshJobSettingsBuilder numberOfNodes(int newNumberOfNodes) {
		this.numberOfNodes = newNumberOfNodes;
		return this;
	}

	public SshJobSettingsBuilder numberOfCoresPerNode(
		int newNumberOfCoresPerNode)
	{
		this.numberOfCoresPerNode = newNumberOfCoresPerNode;
		return this;
	}

	public SshJobSettingsBuilder queueOrPartition(String newQueueOrPartition) {
		this.queueOrPartition = newQueueOrPartition;
		return this;
	}

	public SshJobSettings build() {
		return new SshJobSettings() {

			@Override
			public int getNumberOfNodes() {
				return numberOfNodes;
			}

			@Override
			public int getNumberOfCoresPerNode() {
				return numberOfCoresPerNode;
			}

			@Override
			public String getQueueOrPartition() {
				return queueOrPartition;
			}
		};
	}
}
