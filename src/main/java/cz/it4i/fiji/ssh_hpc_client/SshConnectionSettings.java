
package cz.it4i.fiji.ssh_hpc_client;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;

import cz.it4i.fiji.hpc_workflow.paradigm_manager.SettingsWithWorkingDirectory;
import lombok.Data;

@Data
public class SshConnectionSettings implements SettingsWithWorkingDirectory {

	private static final long serialVersionUID = 1L;

	private String host;
	private int port;
	private AuthenticationChoice authenticationChoice;
	private String userName;
	private String password;
	private File keyFile;
	private String keyFilePassword;
	private String schedulerType;

	public SshConnectionSettings(String host, int port,
		AuthenticationChoice authenticationChoice, String userName, String password,
		File keyFile, String keyFilePassword, String schedulerType)
	{
		this.setHost(host);
		this.setPort(port);
		this.authenticationChoice = authenticationChoice;
		this.setUserName(userName);
		this.password = password;
		this.keyFile = keyFile;
		this.keyFilePassword = keyFilePassword;
		this.schedulerType = schedulerType;
	}

	@Override
	public Path getWorkingDirectory() {
		// TODO Auto-generated method stub
		return null;
	}

}
