
package cz.it4i.fiji.ssh_hpc_client;

import java.io.File;
import java.io.Serializable;

import cz.it4i.fiji.ssh_hpc_client.AuthenticationChoice;
import lombok.Data;

@Data
public class SshConnectionSettings implements Serializable {

	private static final long serialVersionUID = 1L;

	private String host;
	private int port;
	private AuthenticationChoice authenticationChoice;
	private String userName;
	private String password;
	private File keyFile;
	private String keyFilePassword;
	private boolean shutdownJobAfterClose;
	private boolean redirectStdOutErr;
	private String schedulerType;

	public SshConnectionSettings(String host, int port,
		AuthenticationChoice authenticationChoice, String userName, String password,
		File keyFile, String keyFilePassword, boolean shutdownJobAfterClose,
		boolean redirectStdOutErr, String schedulerType)
	{
		this.setHost(host);
		this.setPort(port);
		this.authenticationChoice = authenticationChoice;
		this.setUserName(userName);
		this.password = password;
		this.keyFile = keyFile;
		this.keyFilePassword = keyFilePassword;
		this.shutdownJobAfterClose = shutdownJobAfterClose;
		this.redirectStdOutErr = redirectStdOutErr;
		this.schedulerType = schedulerType;
	}

}
