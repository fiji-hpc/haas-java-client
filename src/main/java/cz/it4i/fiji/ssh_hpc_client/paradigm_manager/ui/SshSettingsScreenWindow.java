
package cz.it4i.fiji.ssh_hpc_client.paradigm_manager.ui;

import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

import cz.it4i.fiji.hpc_workflow.ui.LastFormLoader;
import cz.it4i.parallel.paradigm_managers.HPCSettings;
import cz.it4i.parallel.paradigm_managers.ParadigmProfileSettingsEditor;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

class SshSettingsScreenWindow {

	@Plugin(type = ParadigmProfileSettingsEditor.class, priority = Priority.HIGH)
	public static class Editor implements ParadigmProfileSettingsEditor<HPCSettings> {

		@Parameter
		private Context context;

		@Override
		public Class<HPCSettings> getTypeOfSettings() {
			return HPCSettings.class;
		}

		@Override
		public HPCSettings edit(HPCSettings settings) {
			SshSettingsScreenWindow hpcSettingsScreenWindow =
				new SshSettingsScreenWindow();
			hpcSettingsScreenWindow.initialize(context.getService(PrefService.class));
			return hpcSettingsScreenWindow.showDialog(settings);
		}
	}

	private SshSettingsScreenController controller;

	private Window owner;

	private PrefService prefService;

	public HPCSettings showDialog(final HPCSettings oldSettings) {
		HPCSettings settings;

		// Get the old settings:
		settings = oldSettings;

		// if the old settings are null set the last time's
		// user approved settings of this form.
		LastFormLoader<HPCSettings> storeLastForm = new LastFormLoader<>(
			prefService, "hpcSettingsForm", this.getClass());
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
			if (oldSettings != null) {
				settings.setJobID(oldSettings.getJobID());
			}
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

	public void setOwner(Window aOwner) {
		this.owner = aOwner;
	}

	private void openWindow() {
		final Scene formScene = new Scene(this.controller);
		final Stage parentStage = new Stage();
		parentStage.initModality(Modality.APPLICATION_MODAL);
		parentStage.setResizable(false);
		parentStage.setTitle("HPC Settings");
		parentStage.setScene(formScene);
		parentStage.initOwner(owner);

		parentStage.showAndWait();
	}

	public void initialize(PrefService newPrefService) {
		if (this.prefService == null) {
			this.prefService = newPrefService;
		}
	}
}
