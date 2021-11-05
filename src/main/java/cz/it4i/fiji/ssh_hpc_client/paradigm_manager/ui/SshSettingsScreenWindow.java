
package cz.it4i.fiji.ssh_hpc_client.paradigm_manager.ui;

import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

import cz.it4i.fiji.ssh_hpc_client.SshConnectionSettings;
import cz.it4i.parallel.internal.ui.LastFormLoader;
import cz.it4i.parallel.paradigm_managers.ParadigmProfileSettingsEditor;
import cz.it4i.swing_javafx_ui.IconHelperMethods;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

class SshSettingsScreenWindow {

	@Plugin(type = ParadigmProfileSettingsEditor.class, priority = Priority.HIGH)
	public static class Editor implements
		ParadigmProfileSettingsEditor<SshConnectionSettings>
	{

		@Parameter
		private Context context;

		@Override
		public Class<SshConnectionSettings> getTypeOfSettings() {
			return SshConnectionSettings.class;
		}

		@Override
		public SshConnectionSettings edit(SshConnectionSettings settings) {
			SshSettingsScreenWindow sshSettingsScreenWindow =
				new SshSettingsScreenWindow();
			sshSettingsScreenWindow.initialize(context.getService(PrefService.class));
			return sshSettingsScreenWindow.showDialog(settings);
		}
	}

	private SshSettingsScreenController controller;

	private Window owner;

	private PrefService prefService;

	public SshConnectionSettings showDialog(
		final SshConnectionSettings oldSettings)
	{
		SshConnectionSettings settings;

		// Get the old settings:
		settings = oldSettings;

		// if the old settings are null set the last time's
		// user approved settings of this form.
		LastFormLoader<SshConnectionSettings> storeLastForm = new LastFormLoader<>(
			prefService, "sshSettingsForm", this.getClass());
		if (settings == null) {
			settings = storeLastForm.loadLastForm();
		}

		// Create controller:
		this.controller = new SshSettingsScreenController();
		// Initialize form values with old settings, last approved or default :
		this.controller.setInitialFormValues(settings);
		// Request new settings from the user:
		this.openWindow();

		// If the user did provide new settings:
		if (this.controller.getSettings() != null) {
			// Set the new settings.
			settings = this.controller.getSettings();
			// Store the settings for this form.
			storeLastForm.storeLastForm(settings);
		}
		else {
			// The user has not accepted any settings and therefore they should be
			// empty or the old ones.
			if (oldSettings != null) {
				settings = oldSettings;
			}
			else {
				settings = null;
			}
		}

		// Return the settings.
		return settings;
	}

	public void setOwner(Window newOwner) {
		this.owner = newOwner;
	}

	private void openWindow() {
		final Scene formScene = new Scene(this.controller);
		final Stage parentStage = new Stage();
		parentStage.initModality(Modality.APPLICATION_MODAL);
		parentStage.setResizable(false);
		parentStage.setTitle("SSH Settings");
		parentStage.setScene(formScene);
		parentStage.initOwner(owner);
		Image myImage = IconHelperMethods.convertIkonToImage(
			MaterialDesign.MDI_SETTINGS);
		parentStage.getIcons().add(myImage);
		parentStage.showAndWait();
	}

	public void initialize(PrefService newPrefService) {
		if (this.prefService == null) {
			this.prefService = newPrefService;
		}
	}
}
