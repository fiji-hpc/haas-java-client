/*******************************************************************************
 * IT4Innovations - National Supercomputing Center
 * Copyright (c) 2017 - 2019 All Right Reserved, https://www.it4i.cz
 *
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE', which is part of this project.
 ******************************************************************************/
package cz.it4i.fiji.ssh_hpc_client.paradigm_manager;

import org.scijava.plugin.Plugin;

import cz.it4i.fiji.heappe_hpc_client.HaaSClientSettingsImpl;
import cz.it4i.fiji.hpc_workflow.paradigm_manager.WorkflowParadigmManager;
import cz.it4i.fiji.ssh_hpc_client.SshHPCClient;

@Plugin(type = WorkflowParadigmManager.class)
public class SshWorkflowParadigmManager extends
	WorkflowParadigmManager<HaaSClientSettingsImpl, SshClientJobSettings>
{

	public SshWorkflowParadigmManager()
	{
		super(HaaSClientSettingsImpl.class,
			SshHPCClient.class,
			SshClientJobSettings.class);
	}

	@Override
	public String toString() {
		return "WorkflowParadigm over ssh";
	}
}
