
package cz.it4i.fiji.ssh_hpc_client.paradigm_manager.ui;

import cz.it4i.swing_javafx_ui.JavaFXRoutines;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Spinner;

public class SimpleControls {

	private SimpleControls() {
		// Private constructor to hide the implicit public one.
	}

	public static void spinnerIgnoreNoneNumericInput(Spinner<Integer> spinner,
		int lowerBound, int upperBound)
	{
		spinner.getEditor().textProperty().addListener((
			ObservableValue<? extends String> observable, String oldValue,
			String newValue) -> {
				// Value should be numeric only:
				if (!newValue.matches("\\d*"))
				{
				JavaFXRoutines.runOnFxThread(() -> spinner.getEditor().setText(newValue
					.replaceAll("[^\\d]", "")));
			}
				// Value should be within the boundaries.
				try
				{
				int temp = Integer.parseInt(newValue);
				if (temp < lowerBound) {
					JavaFXRoutines.runOnFxThread(() -> spinner.getEditor().setText("" +
						lowerBound));
				}
				else if (temp > upperBound) {
					JavaFXRoutines.runOnFxThread(() -> spinner.getEditor().setText("" +
						upperBound));
				}
			}
			catch (Exception e) {
				// Value should be an integer.
				JavaFXRoutines.runOnFxThread(() -> spinner.getEditor().setText("" +
					oldValue));
			}
			});
	}
}
