
package cz.it4i.fiji.ssh_hpc_client.paradigm_manager.ui;

import java.io.File;
import cz.it4i.fiji.ssh_hpc_client.AuthenticationChoice;
import cz.it4i.fiji.ssh_hpc_client.SshConnectionSettings;
import cz.it4i.swing_javafx_ui.JavaFXRoutines;
import cz.it4i.swing_javafx_ui.SimpleDialog;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
	private TextField commandTextField;

	@FXML
	private TextField remoteWorkingDirectoryTextField;

	@Getter
	@Setter
	private SshConnectionSettings settings;

	private static final Integer PORT_DEFAULT_VALUE = 1;

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
		portSpinner.getEditor().textProperty().addListener((
			ObservableValue<? extends String> observable, String oldValue,
			String newValue) -> {
				// Value should be numeric only:
				if (!newValue.matches("\\d*"))
				{
				portSpinner.getEditor().setText(newValue.replaceAll("[^\\d]", ""));
			}
				// Value should be within the boundaries.
				try
				{
				int temp = Integer.parseInt(newValue);
				if (temp < PORT_LOWER_BOUND) {
					portSpinner.getEditor().setText("" + PORT_LOWER_BOUND);
				}
				else if (temp > PORT_UPPER_BOUND) {
					portSpinner.getEditor().setText("" + PORT_UPPER_BOUND);
				}
			}
			catch (Exception e) {
				// Value should be an integer.
				portSpinner.getEditor().setText(oldValue);
			}
			});

		SpinnerValueFactory<Integer> portValueFactory =
			new SpinnerValueFactory.IntegerSpinnerValueFactory(PORT_LOWER_BOUND,
				PORT_UPPER_BOUND, PORT_DEFAULT_VALUE);
		portSpinner.setValueFactory(portValueFactory);

		// Disable fields that are not relevant to authentication method selection:
		authenticationChoiceKeyRadioButton.selectedProperty().addListener((
			ObservableValue<? extends Boolean> obs, Boolean wasPreviouslySelected,
			Boolean isNowSelected) -> disableIrrelevantFileds(isNowSelected));
	}

	public void disableIrrelevantFileds(Boolean isSelected) {
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
			"Open SSH Public Key file");
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
		this.settings = createSettings();
		((Stage) getScene().getWindow()).close();
	}

	private SshConnectionSettings createSettings() {
		String host = hostTextField.getText();
		commitSpinnerValue(portSpinner);
		int port = portSpinner.getValue();

		AuthenticationChoice authenticationChoice;
		if (authenticationChoiceKeyRadioButton.isSelected()) {
			authenticationChoice = AuthenticationChoice.KEY_FILE;
		}
		else {
			authenticationChoice = AuthenticationChoice.PASSWORD;
		}

		String userName = userNameTextField.getText();
		String password = passwordPasswordField.getText();
		File keyFile = new File(keyFileTextField.getText());
		String keyFilePassword = keyFilePasswordPasswordField.getText();
		String remoteFijiDirectory = remoteDirectoryTextField.getText();
		String command = commandTextField.getText();
		String workingDirectory = workingDirectoryTextField.getText();
		String remoteWorkingDirectory = remoteWorkingDirectoryTextField.getText();

		return new SshConnectionSettings(host, port, authenticationChoice, userName,
			password, keyFile, keyFilePassword, workingDirectory, remoteFijiDirectory,
			command, remoteWorkingDirectory);
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

	public void setInitialFormValues(SshConnectionSettings oldSettings) {
		if (oldSettings == null) {
			hostTextField.setText("localhost");
			portSpinner.getValueFactory().setValue(PORT_DEFAULT_VALUE);
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
			commandTextField.setText(oldSettings.getCommand());
			remoteWorkingDirectoryTextField.setText(oldSettings
				.getRemoteWorkingDirectory());
		}
	}
}
