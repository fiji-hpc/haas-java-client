
package cz.it4i.fiji.ssh_hpc_client.paradigm_manager.ui;

import cz.it4i.cluster_job_launcher.HPCSchedulerType;
import cz.it4i.fiji.ssh_hpc_client.AdvancedSettings;
import cz.it4i.fiji.ssh_hpc_client.SshConnectionSettings;
import cz.it4i.swing_javafx_ui.JavaFXRoutines;
import cz.it4i.swing_javafx_ui.SimpleDialog;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

public class SshAdvancedSettingsScreenController extends AnchorPane {

	@FXML
	private TextField commandTextField;

	@FXML
	private ComboBox<String> jobSchedulerComboBox;

	@FXML
	private TextField openMpiModuleTextField;

	@FXML
	private Button okButton;

	@Getter
	@Setter
	private AdvancedSettings advancedSettings;

	// Human readable item names for the job scheduler combo box:
	private static final String PBS_ITEM = "PBS Professional";
	private static final String SLURM_ITEM = "Slurm Workload Manager";
	private static final String IBM_ITEM = "IBM Spectrum LSF";

	public SshAdvancedSettingsScreenController() {
		JavaFXRoutines.initRootAndController("SshAdvancedSettingsScreen.fxml",
			this);
	}

	@FXML
	public void initialize() {
		jobSchedulerComboBox.getItems().addAll(PBS_ITEM, SLURM_ITEM, IBM_ITEM);
		jobSchedulerComboBox.getSelectionModel().selectFirst();
	}

	// Disable fields if automatic advanced settings detection is selected:
	public void setAutomatic(boolean isSelected) {
		commandTextField.setDisable(isSelected);
		openMpiModuleTextField.setDisable(isSelected);
		jobSchedulerComboBox.setDisable(isSelected);
	}

	@FXML
	private void okAction() {
		this.advancedSettings = createSettings();

		// If the fields are disabled or all of the files are filled-out allow the
		// user to close the window.
		if (advancedSettings.getCommand() != null && advancedSettings
			.getJobScheduler() != null && advancedSettings
				.getOpenMpiModule() != null && !advancedSettings.getCommand()
					.isEmpty() && !advancedSettings.getOpenMpiModule().isEmpty() ||
			commandTextField.isDisabled())
		{
			((Stage) getScene().getWindow()).close();
		}
		else {
			SimpleDialog.showWarning("Fill out form",
				"Please fill out all fields of the form.");
		}
	}

	private HPCSchedulerType comboBoxIdToJobScheduler(String id) {
		if (id.equals(PBS_ITEM)) {
			return HPCSchedulerType.PBS;
		}
		else if (id.equals(SLURM_ITEM)) {
			return HPCSchedulerType.SLURM;
		}
		else if (id.equals(IBM_ITEM)) {
			return HPCSchedulerType.LSF;
		}
		return null;
	}

	private String jobSchedulerToComboBoxId(HPCSchedulerType jobScheduler) {
		if (jobScheduler == HPCSchedulerType.PBS) {
			return PBS_ITEM;
		}
		else if (jobScheduler == HPCSchedulerType.SLURM) {
			return SLURM_ITEM;
		}
		else if (jobScheduler == HPCSchedulerType.LSF) {
			return IBM_ITEM;
		}
		else {
			// Set PBS in case of no data as a default option.
			return PBS_ITEM;
		}
	}

	private AdvancedSettings createSettings() {
		String command = commandTextField.getText();
		String openMpiModule = openMpiModuleTextField.getText();
		String jobScheduler = jobSchedulerComboBox.getValue();

		return new AdvancedSettings(command, openMpiModule,
			comboBoxIdToJobScheduler(jobScheduler));
	}

	public void setInitialFormValues(SshConnectionSettings oldSettings,
		AdvancedSettings advancedSettings)
	{
		// If this is the second time the user edits the advanced settings get the
		// previous values:
		if (advancedSettings != null) {
			this.advancedSettings = advancedSettings;
			commandTextField.setText(advancedSettings.getCommand());
			openMpiModuleTextField.setText(advancedSettings.getOpenMpiModule());
			jobSchedulerComboBox.getSelectionModel().select(jobSchedulerToComboBoxId(
				advancedSettings.getJobScheduler()));
		}
		// If this paradigm profile already exist get the old settings.
		else if (oldSettings != null) {
			commandTextField.setText(oldSettings.getCommand());
			openMpiModuleTextField.setText(oldSettings.getOpenMpiModule());
			jobSchedulerComboBox.getSelectionModel().select(jobSchedulerToComboBoxId(
				oldSettings.getJobScheduler()));
		}
		else {
			// Do nothing. There are no previous values to set.
		}
	}
}
