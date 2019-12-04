package cz.it4i.fiji.heappe_hpc_client;

import cz.it4i.fiji.hpc_client.HPCClientException;

class HaaSClientException extends HPCClientException {

	private static final long serialVersionUID = 1L;

	public HaaSClientException() {
	}

	public HaaSClientException(String message) {
		super(message);
	}

	public HaaSClientException(Throwable cause) {
		super(cause);
	}

	public HaaSClientException(String message, Throwable cause) {
		super(message, cause);
	}

	public HaaSClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
