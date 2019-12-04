package cz.it4i.fiji.heappe_hpc_client;

class Constants extends Configuration{

	
	public Constants(String configFileName) {
		super(configFileName);
	}

	public String getUserName() {
		return getValue("USER_NAME");
	}

	public String getPhone() {
		return getValue("PHONE");
	}

	public String getPassword() {
		return getValue("PASSWORD");
	}

	public String getEmail() {
		return getValue("EMAIL");
	}
	
	
	
}