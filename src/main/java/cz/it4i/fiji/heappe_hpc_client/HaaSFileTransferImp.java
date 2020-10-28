
package cz.it4i.fiji.heappe_hpc_client;

import com.jcraft.jsch.JSchException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.fiji.haas_java_client.proxy.FileTransferMethodExt;
import cz.it4i.fiji.hpc_client.HPCFileTransfer;
import cz.it4i.fiji.hpc_client.UploadingFile;
import cz.it4i.fiji.scpclient.ScpClient;
import cz.it4i.fiji.scpclient.TransferFileProgress;

public class HaaSFileTransferImp implements HPCFileTransfer {

	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(
		cz.it4i.fiji.heappe_hpc_client.HaaSFileTransferImp.class);

	private final ScpClient scpClient;
	private TransferFileProgress progress;
	private String remoteWorkingDirectory;

	// Constructor for HEAppE use:
	public HaaSFileTransferImp(FileTransferMethodExt ft, ScpClient scpClient,
		TransferFileProgress progress)
	{
		this.remoteWorkingDirectory = ft.getSharedBasepath();
		this.scpClient = scpClient;
		this.progress = progress;
	}
	
	// Constructor for SSH use:
	public HaaSFileTransferImp(String newRemoteWorkingDirectory,
		ScpClient newScpClient, TransferFileProgress newProgress)
	{
		this.remoteWorkingDirectory = newRemoteWorkingDirectory;
		this.scpClient = newScpClient;
		this.progress = newProgress;
	}

	@Override
	public void upload(final UploadingFile file) throws InterruptedIOException {
		String destFile = this.remoteWorkingDirectory + "/" + file.getName();
		destFile = destFile.replace("\\", "/");
		try (InputStream is = file.getInputStream()) {
			scpClient.upload(is, destFile, file.getLength(), file.getLastTime(),
				progress);
		}
		catch (JSchException | IOException e) {
			throw new HaaSClientException("An upload of " + file + " to " + destFile +
				" failed: " + e.getMessage(), e);
		}
	}

	@Override
	public void download(String fileName, final Path workDirectory)
		throws InterruptedIOException
	{
		try {
			final Path rFile = workDirectory.resolve(fileName);
			final String fileToDownload = this.remoteWorkingDirectory + "/" + fileName;
			scpClient.download(fileToDownload, rFile, progress);
		}
		catch (JSchException | IOException e) {
			throw new HaaSClientException("A download of " + fileName + " to " +
				workDirectory + " failed: " + e.getMessage());
		}
	}

	@Override
	public void setProgress(TransferFileProgress progress) {
		this.progress = progress;
	}

	@Override
	public List<Long> obtainSize(List<String> files)
		throws InterruptedIOException
	{
		try {
			return getSizes(files.stream().map(filename -> this.remoteWorkingDirectory +
				"/" + filename).collect(Collectors.toList()));
		}
		catch (InterruptedIOException e) {
			throw e;
		}
		catch (JSchException | IOException e) {
			throw new HaaSClientException(e);
		}

	}

	// FIXME: merge with download - stream provider for file, consumer for stream
	@Override
	public List<String> getContent(List<String> files) {
		List<String> result = new LinkedList<>();
		try {
			for (String fileName : files) {
				fileName = replaceIfFirstFirst(fileName);
				try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
					String fileToDownload = this.remoteWorkingDirectory + "/" + fileName;
					scpClient.download(fileToDownload, os, progress);
					os.flush();
					result.add(os.toString());
				}
			}
		}
		catch (JSchException | IOException e) {
			throw new HaaSClientException(e);
		}
		return result;
	}

	private String replaceIfFirstFirst(String fileName) {
		if (fileName.length() < 0 && fileName.charAt(0) == '/') {
			fileName = fileName.substring(1);
		}
		return fileName;
	}

	private List<Long> getSizes(List<String> asList) throws JSchException,
		IOException
	{
		List<Long> result = new LinkedList<>();
		for (String lfile : asList) {
			result.add(scpClient.size(lfile));
		}
		return result;
	}

}
