
package cz.it4i.fiji.ssh_hpc_client.paradigm_manager.ui;

import cz.it4i.swing_javafx_ui.JavaFXRoutines;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class PreviewSubmitCommandScreenController extends AnchorPane {

	@FXML
	private TextArea previewTextArea;

	@FXML
	private Button closeButton;

	@FXML
	private Button copyToClipboardButton;

	private String command;

	public PreviewSubmitCommandScreenController() {
		JavaFXRoutines.initRootAndController("PreviewSubmitCommandScreen.fxml",
			this);
	}

	@FXML
	public void initialize() {
		// Nothing to initialise.
	}

	@FXML
	private void closeAction() {
		((Stage) getScene().getWindow()).close();
	}

	@FXML
	private void copyToClipboardAction() {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();
		content.putString(command);
		clipboard.setContent(content);
	}

	public void setInitialFormValues(String command) {
		this.command = command;
		this.previewTextArea.setText(command);
	}
}
