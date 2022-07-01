
package cz.it4i.fiji.ssh_hpc_client.paradigm_manager.ui;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import cz.it4i.cluster_job_launcher.AuthenticationChoice;
import cz.it4i.cluster_job_launcher.HPCSchedulerType;
import cz.it4i.fiji.ssh_hpc_client.AdvancedSettings;
import cz.it4i.fiji.ssh_hpc_client.SshConnectionSettings;
import cz.it4i.swing_javafx_ui.JavaFXRoutines;
import cz.it4i.swing_javafx_ui.SimpleControls;
import cz.it4i.swing_javafx_ui.SimpleDialog;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.Setter;

public class SshSettingsScreenController extends AnchorPane {

	@FXML
	private TextField hostTextField;

	@FXML
	private Spinner<Integer> portSpinner;

	@FXML
	private TextField userNameTextField;

	@FXML
	private ToggleGroup authenticationMethod;

	@FXML
	private RadioButton authenticationChoiceKeyRadioButton;

	@FXML
	private RadioButton authenticationChoicePasswordRadioButton;

	@FXML
	private TextField keyFileTextField;

	@FXML
	private PasswordField keyFilePasswordPasswordField;

	@FXML
	private PasswordField passwordPasswordField;

	@FXML
	private Button okButton;

	@FXML
	private Button browseButton;

	@FXML
	private TextField workingDirectoryTextField;

	@FXML
	private Button workingDirectoryBrowseButton;

	@FXML
	private TextField remoteDirectoryTextField;

	@FXML
	private TextField remoteWorkingDirectoryTextField;

	@FXML
	private Hyperlink advancedOptionsHyperlink;

	@FXML
	private CheckBox automaticAdvancedSettingsCheckBox;

	@Getter
	@Setter
	private SshConnectionSettings settings;

	private SshConnectionSettings oldSettings;

	private AdvancedSettings advancedSettings = null;

	private static final Integer PORT_DEFAULT_VALUE = 22;

	private static final int PORT_LOWER_BOUND = 1;
	private static final int PORT_UPPER_BOUND = 65535;

	public SshSettingsScreenController() {
		JavaFXRoutines.initRootAndController("SshSettingsScreen.fxml", this);
	}

	@FXML
	public void initialize() {
		// RadioButtons:
		authenticationChoiceKeyRadioButton.setSelected(true);

		// Spinners:
		SimpleControls.spinnerIgnoreNoneNumericInput(portSpinner, PORT_LOWER_BOUND,
			PORT_UPPER_BOUND);
		SpinnerValueFactory<Integer> portValueFactory =
			new SpinnerValueFactory.IntegerSpinnerValueFactory(PORT_LOWER_BOUND,
				PORT_UPPER_BOUND, PORT_DEFAULT_VALUE);
		portSpinner.setValueFactory(portValueFactory);

		// Disable fields that are not relevant to authentication method selection:
		authenticationChoiceKeyRadioButton.selectedProperty().addListener((
			ObservableValue<? extends Boolean> obs, Boolean wasPreviouslySelected,
			Boolean isNowSelected) -> disableIrrelevantFileds(isNowSelected));

		// If the user selects to manually select the advanced options open the
		// window.
		automaticAdvancedSettingsCheckBox.selectedProperty().addListener((
			ObservableValue<? extends Boolean> observableValue, Boolean oldValue,
			Boolean newValue) -> {
			if (newValue.booleanValue()) {
				advancedOptionsHyperlink.setDisable(true);
			}
			else {
				advancedOptionsHyperlink.setDisable(false);
				showAdvancedSettings();
			}
		});
	}

	public void disableIrrelevantFileds(boolean isSelected) {
		if (isSelected) {
			passwordPasswordField.setDisable(true);
			keyFileTextField.setDisable(false);
			keyFilePasswordPasswordField.setDisable(false);
			browseButton.setDisable(false);
		}
		else {
			keyFileTextField.setDisable(true);
			keyFilePasswordPasswordField.setDisable(true);
			browseButton.setDisable(true);
			passwordPasswordField.setDisable(false);
		}
	}

	@FXML
	private void browseFileAction() {
		Stage stage = (Stage) browseButton.getScene().getWindow();
		File selectedFile = SimpleDialog.fileChooser(stage,
			"Open SSH Private Key file");
		if (selectedFile != null) {
			this.keyFileTextField.setText(selectedFile.getAbsolutePath());
		}
	}

	@FXML
	private void browseDirectoryAction() {
		Stage stage = (Stage) browseButton.getScene().getWindow();
		File selectedDirectory = SimpleDialog.directoryChooser(stage,
			"Open Working Directory");
		if (selectedDirectory != null && selectedDirectory.isDirectory()) {
			this.workingDirectoryTextField.setText(selectedDirectory
				.getAbsolutePath());
		}
	}

	@FXML
	private void okAction() {
		if (settingsAreValid()) {
			this.settings = createSettings();
			((Stage) getScene().getWindow()).close();
		}
	}

	private boolean settingsAreValid() {
		String hostname = hostTextField.getText();
		String localWorkingDirectory = workingDirectoryTextField.getText().trim();
		String remoteWorkingDirectory = remoteWorkingDirectoryTextField.getText().trim();
		String keyFilePath = keyFileTextField.getText().trim();
		String remoteFijiDirectory = remoteDirectoryTextField.getText();

		String title = "";
		String message = "";
		boolean valid = true;
		if (hostname.trim().isEmpty()) {
			title = "Host name should not be empty.";
			message = "You must provide a valid host name.";
			valid = false;
		}
		else if (userNameTextField.getText().trim().isEmpty()) {
			title = "Username should not be empty.";
			message = "You must provide a user name.";
			valid = false;
		}
		else if (authenticationChoiceKeyRadioButton.isSelected() && keyFilePath
			.isEmpty())
		{
			title = "Key file should be provided.";
			message = "You must provide the path to a valid RSA private key file.";
			valid = false;
		}
		else if (authenticationChoiceKeyRadioButton.isSelected() &&
			!fileExistsLocaly(keyFilePath))
		{
			title = "Key file must exist.";
			message = "The key file you selected does not exist.";
			valid = false;
		}
		else if (remoteWorkingDirectory.isEmpty()) {
			title = "The remote direcotry path should not be empty.";
			message = "You must provide a valid remote working directory path.";
			valid = false;
		}
		else if (localWorkingDirectory.isEmpty()) {
			title = "The local working directory path should not be empty.";
			message = "Select a valid path on your computer.";
			valid = false;
		}
		else if (remoteWorkingDirectory.equals(remoteFijiDirectory)) {
			title =
				"The remote working directory and remote Fiji directory must be different.";
			message =
				"Use a different remote working directory or remote Fiji directory.";
			valid = false;
		}
		else if (!pathIsValid(remoteWorkingDirectory)) {
			title = "The path of the remote working directory is invalid.";
			message = "Specify a valid remote working direcory.";
			valid = false;
		}
		else if (!pathExistsLocaly(localWorkingDirectory)) {
			title = "The local working directory you selected does not exist.";
			message = "Select an existing local directory.";
			valid = false;
		}

		if (!valid) {
			SimpleDialog.showWarning(title, message);
		}

		return valid;
	}

	private boolean pathIsValid(String pathString) {
		boolean valid = true;
		try {
			Paths.get(pathString);
		}
		catch (InvalidPathException | NullPointerException ex) {
			valid = false;
		}
		return valid;
	}

	private boolean pathExistsLocaly(String pathString) {
		boolean valid = true;
		if (pathIsValid(pathString)) {
			File file = new File(pathString);
			valid = file.exists() && file.isDirectory();
		}
		else {
			valid = false;
		}

		return valid;
	}

	private boolean fileExistsLocaly(String pathString) {
		boolean valid = true;
		if (pathIsValid(pathString)) {
			File file = new File(pathString);
			valid = file.exists() && file.isFile();
		}
		else {
			valid = false;
		}

		return valid;
	}

	private SshConnectionSettings createSettings() {
		String host = hostTextField.getText().trim();
		commitSpinnerValue(portSpinner);
		int port = portSpinner.getValue();

		AuthenticationChoice authenticationChoice;
		if (authenticationChoiceKeyRadioButton.isSelected()) {
			authenticationChoice = AuthenticationChoice.KEY_FILE;
		}
		else {
			authenticationChoice = AuthenticationChoice.PASSWORD;
		}

		String userName = userNameTextField.getText().trim();
		String password = passwordPasswordField.getText();
		File keyFile = new File(keyFileTextField.getText().trim());
		String keyFilePassword = keyFilePasswordPasswordField.getText();
		String remoteFijiDirectory = remoteDirectoryTextField.getText();
		String workingDirectory = workingDirectoryTextField.getText().trim();
		String remoteWorkingDirectory = remoteWorkingDirectoryTextField.getText().trim();

		// Get command from advanced settings if set, else keep the old value:
		String command = null;
		String openMpiModule = null;
		HPCSchedulerType jobScheduler = null;

		// Delete the advanced settings if the user has chosen not to use
		// automatic configuration:
		if (!automaticAdvancedSettingsCheckBox.isSelected()) {
			if (advancedSettings != null) {
				command = advancedSettings.getCommand();
				openMpiModule = advancedSettings.getOpenMpiModule();
				jobScheduler = advancedSettings.getJobScheduler();
			}
			else if (oldSettings != null) {
				command = oldSettings.getCommand();
				openMpiModule = oldSettings.getOpenMpiModule();
				jobScheduler = oldSettings.getJobScheduler();
			}
		}

		return new SshConnectionSettings(host, port, authenticationChoice, userName,
			password, keyFile, keyFilePassword, workingDirectory, remoteFijiDirectory,
			command, remoteWorkingDirectory, openMpiModule, jobScheduler);
	}

	private <T> void commitSpinnerValue(Spinner<T> spinner) {
		if (!spinner.isEditable()) return;
		String text = spinner.getEditor().getText();
		SpinnerValueFactory<T> valueFactory = spinner.getValueFactory();
		if (valueFactory != null) {
			StringConverter<T> converter = valueFactory.getConverter();
			if (converter != null) {
				T value = converter.fromString(text);
				valueFactory.setValue(value);
			}
		}
	}

	@FXML
	private void showAdvancedSettings() {
		SshAdvancedSettingsScreenWindow sshAdvancedSettingsScreenWindow =
			new SshAdvancedSettingsScreenWindow();
		this.advancedSettings = sshAdvancedSettingsScreenWindow.showDialog(
			this.oldSettings, advancedSettings, automaticAdvancedSettingsCheckBox
				.isSelected());
		if (this.advancedSettings.isEmpty()) {
			advancedOptionsHyperlink.setDisable(true);
			automaticAdvancedSettingsCheckBox.setSelected(true);
		}
	}

	public void setInitialFormValues(SshConnectionSettings oldSettings) {
		this.oldSettings = oldSettings;

		if (oldSettings == null) {
			hostTextField.setText("localhost");
			portSpinner.getValueFactory().setValue(PORT_DEFAULT_VALUE);
			// Set key file authentication as default selection:
			authenticationChoiceKeyRadioButton.setSelected(true);
			authenticationChoicePasswordRadioButton.setSelected(false);
			disableIrrelevantFileds(true);
			automaticAdvancedSettingsCheckBox.setSelected(true);
		}
		else {
			hostTextField.setText(oldSettings.getHost());
			portSpinner.getValueFactory().setValue(oldSettings.getPort());
			userNameTextField.setText(oldSettings.getUserName());
			// Get authentication choice:
			if (oldSettings
				.getAuthenticationChoice() == AuthenticationChoice.KEY_FILE)
			{
				authenticationChoiceKeyRadioButton.setSelected(true);
				authenticationChoicePasswordRadioButton.setSelected(false);
				disableIrrelevantFileds(true);
			}
			else {
				authenticationChoiceKeyRadioButton.setSelected(false);
				authenticationChoicePasswordRadioButton.setSelected(true);
				disableIrrelevantFileds(false);
			}
			keyFileTextField.setText(oldSettings.getKeyFile().getAbsolutePath());
			keyFilePasswordPasswordField.setText(oldSettings.getKeyFilePassword());
			passwordPasswordField.setText(oldSettings.getPassword());
			workingDirectoryTextField.setText(oldSettings.getWorkingDirectory()
				.toAbsolutePath().toString());
			remoteDirectoryTextField.setText(oldSettings.getRemoteDirectory());
			remoteWorkingDirectoryTextField.setText(oldSettings
				.getRemoteWorkingDirectory());

			if (oldSettings.getCommand() == null && oldSettings
				.getOpenMpiModule() == null && oldSettings.getJobScheduler() == null)
			{
				automaticAdvancedSettingsCheckBox.setSelected(true);
			}
		}
	}
}
