/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2019 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.heappe_hpc_client.paradigm_manager;

import org.scijava.plugin.Plugin;

import cz.it4i.fiji.heappe_hpc_client.HaaSClient;
import cz.it4i.fiji.heappe_hpc_client.HaaSClientSettingsImpl;
import cz.it4i.fiji.hpc_client.HPCClient;
import cz.it4i.fiji.hpc_workflow.paradigm_manager.WorkflowParadigmManager;

@Plugin(type = WorkflowParadigmManager.class)
public class HEAppEWorkflowParadigmManager extends
	WorkflowParadigmManager<HaaSClientSettingsImpl, HEAppEClientJobSettings>
{

	public HEAppEWorkflowParadigmManager()
	{
		super(HaaSClientSettingsImpl.class,
			castHaaSClient(HaaSClient.class),
			HEAppEClientJobSettings.class);
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends HPCClient<HEAppEClientJobSettings>>
		castHaaSClient(Class<HaaSClient> class1)
	{
		return (Class<? extends HPCClient<HEAppEClientJobSettings>>) class1;
	}

	@Override
	public String toString() {
		return "WorkflowParadigm over HEAppE";
	}
}
