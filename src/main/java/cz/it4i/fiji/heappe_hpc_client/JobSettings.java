
package cz.it4i.fiji.heappe_hpc_client;

import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;

public interface JobSettings {

	long getTemplateId();

	int getWalltimeLimit();

	long getClusterNodeType();

	String getJobName();

	int getNumberOfNodes();

	int getNumberOfCoresPerNode();

	default Collection<Entry<String, String>> getTemplateParameters() {
		return Collections.emptyList();
	}

	String getUserScriptName();
}
