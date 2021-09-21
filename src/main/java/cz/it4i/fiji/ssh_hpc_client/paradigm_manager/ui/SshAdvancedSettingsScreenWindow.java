
package cz.it4i.fiji.ssh_hpc_client.paradigm_manager.ui;

import org.kordamp.ikonli.materialdesign.MaterialDesign;

import cz.it4i.fiji.ssh_hpc_client.AdvancedSettings;
import cz.it4i.fiji.ssh_hpc_client.SshConnectionSettings;
import cz.it4i.swing_javafx_ui.IconHelperMethods;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

class SshAdvancedSettingsScreenWindow {

	private SshAdvancedSettingsScreenController controller;

	private Window owner;

	public AdvancedSettings showDialog(final SshConnectionSettings oldSettings,
		AdvancedSettings advancedSettings, boolean automaticAdvancedSettings)
	{
		// Create controller:
		this.controller = new SshAdvancedSettingsScreenController();
		// Initialise form values with old settings, last approved or default :
		this.controller.setInitialFormValues(oldSettings, advancedSettings);
		this.controller.setAutomatic(automaticAdvancedSettings);
		// Request new advanced settings from the user:
		this.openWindow();

		// If the user did provide new settings:
		if (this.controller.getAdvancedSettings() != null) {
			// Set the new settings.
			advancedSettings = this.controller.getAdvancedSettings();
		}
		else {
			// The user has not accepted any settings and therefore they should be
			// empty or the old ones.
			if (oldSettings != null) {
				advancedSettings = new AdvancedSettings(oldSettings.getCommand(),
					oldSettings.getOpenMpiModule(), oldSettings.getJobScheduler());
			}
			else {
				advancedSettings = new AdvancedSettings(null, null, null);
			}
		}

		// Return the settings.
		return advancedSettings;
	}

	public void setOwner(Window newOwner) {
		this.owner = newOwner;
	}

	private void openWindow() {
		final Scene formScene = new Scene(this.controller);
		final Stage parentStage = new Stage();
		parentStage.initModality(Modality.APPLICATION_MODAL);
		parentStage.setResizable(false);
		parentStage.setTitle("SSH Advanced Settings");
		parentStage.setScene(formScene);
		parentStage.initOwner(owner);
		Image myImage = IconHelperMethods.convertIkonToImage(
			MaterialDesign.MDI_SETTINGS);
		parentStage.getIcons().add(myImage);
		parentStage.showAndWait();
	}
}
