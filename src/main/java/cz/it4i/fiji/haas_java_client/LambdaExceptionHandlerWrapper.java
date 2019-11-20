package cz.it4i.fiji.haas_java_client;

class LambdaExceptionHandlerWrapper {

	interface Runnable {
		void run() throws Exception;
	}

	static void wrap(Runnable r) {
		try {
			r.run();
		} catch (Exception e) {
			//ignore
		}
	}
}
