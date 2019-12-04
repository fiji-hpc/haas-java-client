package cz.it4i.fiji.heappe_hpc_client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import cz.it4i.fiji.hpc_client.UploadingFile;

class UploadingFileData implements UploadingFile {

	private final byte[] data;
	private final String name;
	
	
	public UploadingFileData(String name, byte[] data) {
		this.data = data;
		this.name = name;
	}

	public UploadingFileData(String string) {
		this(string, new byte[0]);
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(data);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public long getLength() throws IOException {
		return data.length;
	}

	@Override
	public long getLastTime() {
		return 0;
	}

}
