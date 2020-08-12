
package cz.it4i.fiji.heappe_hpc_client;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map.Entry;

public class JobSettingsBuilder {

	private static final long DEFAULT_TEMPLATE = 1l;

	private static final int DEFAULT_WALLTIME = 600;

	private static final long DEFAULT_CLUSTER_NODE_TYPE = 7L;

	private static final String DEFAULT_JOB_NAME = "DefaultHEAppEJob";

	private static final int DEFAULT_NUMBER_OF_NODES = 1;

	private static final int DEFAULT_NUMBER_OF_CORES_PER_NODE = 24;

	private long templateId = DEFAULT_TEMPLATE;
	private int walltimeLimit = DEFAULT_WALLTIME;
	private long clusterNodeType = DEFAULT_CLUSTER_NODE_TYPE;
	private String jobName = DEFAULT_JOB_NAME;
	private int numberOfNodes = DEFAULT_NUMBER_OF_NODES;
	private int numberOfCoresPerNode = DEFAULT_NUMBER_OF_CORES_PER_NODE;
	private String userScriptName;

	private Collection<Entry<String, String>> templateParameters = Collections
		.emptyList();

	public JobSettingsBuilder templateId(long newTemplateId) {
		this.templateId = newTemplateId;
		return this;
	}

	public JobSettingsBuilder walltimeLimit(int newWalltimeLimit) {
		this.walltimeLimit = newWalltimeLimit;
		return this;
	}

	public JobSettingsBuilder clusterNodeType(long newClusterNodeType) {
		this.clusterNodeType = newClusterNodeType;
		return this;
	}

	public JobSettingsBuilder jobName(String newJobName) {
		this.jobName = newJobName;
		return this;
	}

	public JobSettingsBuilder numberOfNodes(int newNumberOfNodes) {
		this.numberOfNodes = newNumberOfNodes;
		return this;
	}

	public JobSettingsBuilder numberOfCoresPerNode(int newNumberOfCoresPerNode) {
		this.numberOfCoresPerNode = newNumberOfCoresPerNode;
		return this;
	}

	public JobSettingsBuilder templateParameters(
		Collection<Entry<String, String>> newTemplateParameters)
	{
		this.templateParameters = new LinkedList<>(newTemplateParameters);
		return this;
	}

	public JobSettings build() {
		return new JobSettings() {

			@Override
			public int getWalltimeLimit() {
				return walltimeLimit;
			}

			@Override
			public long getTemplateId() {
				return templateId;
			}

			@Override
			public int getNumberOfNodes() {
				return numberOfNodes;
			}

			@Override
			public String getJobName() {
				return jobName;
			}

			@Override
			public long getClusterNodeType() {
				return clusterNodeType;
			}

			@Override
			public int getNumberOfCoresPerNode() {
				return numberOfCoresPerNode;
			}

			@Override
			public Collection<Entry<String, String>> getTemplateParameters() {
				return templateParameters;
			}

			@Override
			public String getUserScriptName() {
				return userScriptName;
			}
		};
	}
}
