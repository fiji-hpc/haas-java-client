
package cz.it4i.fiji.ssh_hpc_client.paradigm_manager.ui;

import org.kordamp.ikonli.materialdesign.MaterialDesign;

import cz.it4i.swing_javafx_ui.IconHelperMethods;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class PreviewSubmitCommandScreenWindow {

	private PreviewSubmitCommandScreenController controller;

	private Window owner;

	public void showDialog(String command)
	{
		// Create controller:
		this.controller = new PreviewSubmitCommandScreenController();
		// Set the text of the preview command:
		this.controller.setInitialFormValues(command);
		this.openWindow();
	}

	public void setOwner(Window newOwner) {
		this.owner = newOwner;
	}

	private void openWindow() {
		final Scene formScene = new Scene(this.controller);
		final Stage parentStage = new Stage();
		parentStage.initModality(Modality.APPLICATION_MODAL);
		parentStage.setResizable(true);
		parentStage.setTitle("Preview Remote Command");
		parentStage.setScene(formScene);
		parentStage.initOwner(owner);
		Image myImage = IconHelperMethods.convertIkonToImage(
			MaterialDesign.MDI_CREATION);
		parentStage.getIcons().add(myImage);
		parentStage.showAndWait();
	}
}
