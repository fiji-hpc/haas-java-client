
package cz.it4i.fiji.heappe_hpc_client.paradigm_manager.ui;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.it4i.cluster_job_launcher.HPCSchedulerType;
import cz.it4i.fiji.hpc_workflow.core.DataLocation;
import cz.it4i.fiji.hpc_workflow.core.JobType;
import cz.it4i.fiji.ssh_hpc_client.SshHPCClient;
import cz.it4i.fiji.ssh_hpc_client.paradigm_manager.ui.PreviewSubmitCommandScreenWindow;
import cz.it4i.swing_javafx_ui.JavaFXRoutines;
import cz.it4i.swing_javafx_ui.SimpleControls;
import cz.it4i.swing_javafx_ui.SimpleDialog;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

public class NewJobController extends BorderPane {

	private static final Runnable EMPTY_NOTIFIER = () -> {};

	public static SshHPCClient sshHpcClient;

	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(NewJobController.class);

	@FXML
	private Button createButton;

	@FXML
	private Button previewRemoteCommandButton;

	@FXML
	private ToggleGroup inputDataLocationToggleGroup;

	@FXML
	private ToggleGroup outputDataLocationToggleGroup;

	@FXML
	private ToggleGroup jobTypeSelectorToggleGroup;

	@FXML
	private RadioButton ownInputRadioButton;

	@FXML
	private RadioButton demoInputDataRadioButton;

	@FXML
	private RadioButton ownOutputRadioButton;

	@FXML
	private RadioButton jobSubdirectoryRadioButton;

	@FXML
	private RadioButton workflowSpimRadioButton;

	@FXML
	private RadioButton macroRadioButton;

	@FXML
	private RadioButton scriptRadioButton;

	@FXML
	private TextField inputDirectoryTextField;

	@FXML
	private TextField outputDirectoryTextField;

	@FXML
	private Spinner<Integer> numberOfNodesSpinner;

	@FXML
	private Button selectInputButton;

	@FXML
	private Button selectOutputButton;

	@FXML
	private Spinner<Integer> numberOfCoresPerNodeSpinner;

	@FXML
	private Label queueOrPartitionLabel;

	@FXML
	private TextField queueOrPartitionTextField;

	@FXML
	private HBox queueOrPartitionHBox;

	@FXML
	private HBox inputSelectionHBox;

	@FXML
	private Spinner<Integer> walltimeHourSpinner;

	@FXML
	private Spinner<Integer> walltimeMinuteSpinner;

	@FXML
	private Spinner<Integer> maxMemoryPerNodeSpinner; // Measured in GB.

	private DataLocation inputDataLocation;

	private DataLocation outputDataLocation;

	private JobType jobType;

	private Stage ownerWindow;

	private Runnable createPressedNotifier;

	private static final int DEFAULT_MAX_MEMORY_LIMIT_PER_NODE = 8; // GBs
	private static final int MIN_MAX_MEMORY_PER_NODE = 1; // GB

	public NewJobController(ConnectionType connectionType) {
		JavaFXRoutines.initRootAndController("NewJobView.fxml", this);
		getStylesheets().add(getClass().getResource("NewJobView.css")
			.toExternalForm());
		createButton.setOnMouseClicked(x -> createPressed());
		inputDataLocationToggleGroup.selectedToggleProperty().addListener((v, old,
			n) -> selected(n, ownInputRadioButton));
		outputDataLocationToggleGroup.selectedToggleProperty().addListener((v, o,
			n) -> selected(n, ownOutputRadioButton));

		// Check which job type is selected:
		workflowSpimRadioButton.selectedProperty().addListener((v, o,
			n) -> selectedSpimWorkflow(n));
		macroRadioButton.selectedProperty().addListener((v, o, n) -> selectedMacro(
			n));
		scriptRadioButton.selectedProperty().addListener((v, o,
			n) -> selectedScript(n));

		initSelectButton(inputDirectoryTextField, selectInputButton);
		initSelectDirectoryButton(outputDirectoryTextField, selectOutputButton);

		// Number of nodes spinner value factory:
		SimpleControls.spinnerIgnoreNoneNumericInput(numberOfNodesSpinner, 1,
			Integer.MAX_VALUE);
		SpinnerValueFactory<Integer> nodesValueFactory =
			new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE,
				1);
		numberOfNodesSpinner.setValueFactory(nodesValueFactory);

		// Number of cores per node spinner value factory:
		SimpleControls.spinnerIgnoreNoneNumericInput(numberOfCoresPerNodeSpinner, 1,
			Integer.MAX_VALUE);
		SpinnerValueFactory<Integer> coresValueFactory =
			new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE,
				24);
		numberOfCoresPerNodeSpinner.setValueFactory(coresValueFactory);

		// Walltime hour spinner value factory:
		SimpleControls.spinnerIgnoreNoneNumericInput(walltimeHourSpinner, 0,
			Integer.MAX_VALUE);
		SpinnerValueFactory<Integer> walltimeHourValueFactory =
			new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE,
				1);
		walltimeHourSpinner.setValueFactory(walltimeHourValueFactory);

		// Walltime minutes spinner value factory:
		SimpleControls.spinnerIgnoreNoneNumericInput(walltimeMinuteSpinner, 0, 59);
		SpinnerValueFactory<Integer> walltimeMinuteValueFactory =
			new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0);
		walltimeMinuteSpinner.setValueFactory(walltimeMinuteValueFactory);

		// Maximum memory limit in GBs:
		SimpleControls.spinnerIgnoreNoneNumericInput(maxMemoryPerNodeSpinner,
			MIN_MAX_MEMORY_PER_NODE, Integer.MAX_VALUE);
		SpinnerValueFactory<Integer> maxMemoryPerNodeValueFactory =
			new SpinnerValueFactory.IntegerSpinnerValueFactory(
				MIN_MAX_MEMORY_PER_NODE, Integer.MAX_VALUE,
				DEFAULT_MAX_MEMORY_LIMIT_PER_NODE);
		maxMemoryPerNodeSpinner.setValueFactory(maxMemoryPerNodeValueFactory);

		// Set the default scheduler value:
		// PBS Professional, the express queue - qexp,
		// SLURM Workload Manager: the batch partition - batch:
		String defaultQueueOrPartition = "Not applicable";
		String label = "Not applicable";
		HPCSchedulerType hpcSchedulerType = sshHpcClient.getSchedulerType();
		if (hpcSchedulerType == HPCSchedulerType.SLURM) {
			defaultQueueOrPartition = "batch";
			label = "SLURM Worklfow Manger partition";
		}
		else if (hpcSchedulerType == HPCSchedulerType.PBS) {
			defaultQueueOrPartition = "qexp";
			label = "PBS Profesional queue";
		}
		else if (hpcSchedulerType == HPCSchedulerType.LSF) {
			defaultQueueOrPartition = "normal";
			label = "IBM Spectrum LSF queue";
		}
		queueOrPartitionTextField.setText(defaultQueueOrPartition);
		queueOrPartitionLabel.setText(label);

		if (connectionType == ConnectionType.MIDDLEWARE) {
			scriptRadioButton.disableProperty().set(true);
			queueOrPartitionHBox.setVisible(false);
			queueOrPartitionLabel.setVisible(false);
			queueOrPartitionTextField.setVisible(false);
			previewRemoteCommandButton.setVisible(false);
		}
		else if (connectionType == ConnectionType.SSH) {
			jobTypeSelectorToggleGroup.selectToggle(macroRadioButton);
			workflowSpimRadioButton.disableProperty().set(true);
			inputSelectionHBox.setDisable(false);
			previewRemoteCommandButton.setVisible(true);
		}
	}

	public void close() {
		// There is nothing to close.
	}

	public void init(Stage parentStage) {
		ownerWindow = parentStage;
	}

	public Path getInputDirectory(Path workingDirectory) {
		return getDirectory(inputDataLocation, inputDirectoryTextField.getText(),
			workingDirectory);
	}

	public Path getOutputDirectory(Path workingDirectory) {
		return getDirectory(outputDataLocation, outputDirectoryTextField.getText(),
			workingDirectory);
	}

	public int getNumberOfNodes() {
		return numberOfNodesSpinner.getValue();
	}

	public int getNumberOfCoresPerNode() {
		return numberOfCoresPerNodeSpinner.getValue();
	}

	public String getQueueOrPartition() {
		return queueOrPartitionTextField.getText();
	}

	public JobType getJobType() {
		return jobType;
	}

	public int[] getWalltime() {
		int[] walltime = new int[2];
		walltime[0] = walltimeHourSpinner.getValue();
		walltime[1] = walltimeMinuteSpinner.getValue();
		return walltime;
	}

	public int getMaxMemoryPerNode() {
		return maxMemoryPerNodeSpinner.getValue();
	}

	public void setCreatePressedNotifier(Runnable createPressedNotifier) {
		if (createPressedNotifier != null) {
			this.createPressedNotifier = createPressedNotifier;
		}
		else {
			this.createPressedNotifier = EMPTY_NOTIFIER;
		}
	}

	private void initSelectButton(TextField textField, Button button) {
		button.setOnAction(x -> {
			Window parent = ownerWindow.getScene().getWindow();

			if (workflowSpimRadioButton.isSelected()) {
				setTextFieldForDirectory(textField, parent);
			}
			else if (macroRadioButton.isSelected()) {
				setTextFieldForMacro(textField, parent);
			}
			else if (scriptRadioButton.isSelected()) {
				setTextFieldForScript(textField, parent);
			}
		});
	}

	private void initSelectDirectoryButton(TextField textField, Button button) {
		button.setOnAction(x -> {
			Window parent = ownerWindow.getScene().getWindow();
			setTextFieldForDirectory(textField, parent);
		});
	}

	private void setTextFieldForDirectory(TextField folderString,
		Window parentWindow)
	{
		// Get the path from the text field and set it as initial path for the
		// file chooser if it exists:
		DirectoryChooser dch = new DirectoryChooser();

		// Set initial directory:
		dch.setInitialDirectory(getPathWithoutFile(folderString.getText()));

		// Set the selected directory as the new content of the text field:
		File selectedDirectory = dch.showDialog(parentWindow);
		if (selectedDirectory != null) {
			folderString.setText(selectedDirectory.toString());
		}
	}

	private void setTextFieldForMacro(TextField folderString, Window parent) {
		String fileTypeDescription = "Fiji Macro script file (*.ijm)";
		String fileType = "*.ijm";
		setTextFieldForFile(folderString, parent, fileTypeDescription, fileType);
	}

	private void setTextFieldForScript(TextField folderString, Window parent) {
		String fileTypeDescription = "Python script file (*.py)";
		String fileType = "*.py";
		setTextFieldForFile(folderString, parent, fileTypeDescription, fileType);
	}

	private void setTextFieldForFile(TextField folderString, Window parent,
		String fileTypeDescription, String fileType)
	{
		// Get the path from the text field an set it as initial path for the
		// file chooser if it exists:
		FileChooser fch = new FileChooser();

		// Set initial directory:
		fch.setInitialDirectory(getPathWithoutFile(folderString.getText()));

		// Restrict file choice to required type only:
		FileChooser.ExtensionFilter extensionFilter =
			new FileChooser.ExtensionFilter(fileTypeDescription, fileType);
		fch.getExtensionFilters().add(extensionFilter);

		// Set the selected directory as the new content of the text field:
		File selectedFile = fch.showOpenDialog(parent);
		if (selectedFile != null && !selectedFile.isDirectory()) {
			folderString.setText(selectedFile.toString());
		}
	}

	private File getPathWithoutFile(String pathString) {
		File currentLocation = null;
		File file = Paths.get(pathString).toAbsolutePath().toFile();
		if (file.exists()) {
			// Remove the file at the end from the current directory if there is one:
			if (file.isFile()) {
				currentLocation = file.getParentFile();
			}
			else {
				currentLocation = file;
			}
		}
		return currentLocation;
	}

	private Path getDirectory(DataLocation dataLocation, String selectedDirectory,
		Path workingDirectory)
	{
		switch (dataLocation) {
			case DEMONSTRATION_ON_SERVER:
				return null;
			case WORK_DIRECTORY:
				return workingDirectory;
			case CUSTOM_DIRECTORY:
				return getPathAndSetUserScriptName(selectedDirectory);
			default:
				throw new UnsupportedOperationException("Not support " + dataLocation);
		}
	}

	private Path getPathAndSetUserScriptName(String selectedDirectory) {
		Path path = Paths.get(selectedDirectory).toAbsolutePath();
		File file = path.toFile();
		if (file.isDirectory()) {
			return path;
		}

		return path.getParent();
	}

	public String getUserScriptName() {
		String selectedDirectory = inputDirectoryTextField.getText();
		Path path = Paths.get(selectedDirectory).toAbsolutePath();
		return path.getFileName().toString();
	}

	private void createPressed() {
		JavaFXRoutines.runOnFxThreadAndWait(() -> {
			obtainValues();
			if (checkDirectoryLocationIfNeeded() && walltimeIsGreaterThanZero() &&
				maxMemoryIsGreaterThanZero())
			{
				// Close stage
				Stage stage = (Stage) createButton.getScene().getWindow();
				stage.close();
				this.createPressedNotifier.run();
			}
		});
	}

	private boolean checkDirectoryLocationIfNeeded() {
		return checkDataLocationValue(inputDataLocation, inputDirectoryTextField
			.getText(), "input") && pathPointsToFile(inputDirectoryTextField
				.getText()) && checkDataLocationValue(outputDataLocation,
					outputDirectoryTextField.getText(), "output");
	}

	private boolean walltimeIsGreaterThanZero() {
		boolean greaterThanZero = (walltimeHourSpinner.getValue() > 0 ||
			walltimeMinuteSpinner.getValue() > 0);
		if (!greaterThanZero) {
			SimpleDialog.showWarning("Incorrect amount of time specified.",
				"Enter an amount of time greater than zero for the amount of time needed.");
		}
		return greaterThanZero;
	}

	private boolean maxMemoryIsGreaterThanZero() {
		boolean greaterThanZero = (maxMemoryPerNodeSpinner.getValue() > 0);
		if (!greaterThanZero) {
			SimpleDialog.showWarning("Incorrect max memory limit specified.",
				"Enter a max memory limit that is greater than zero.");
		}
		return greaterThanZero;
	}

	private boolean checkDataLocationValue(DataLocation dataLocation,
		String directory, String type)
	{
		Path directoryPath = Paths.get(directory);
		if (dataLocation == DataLocation.CUSTOM_DIRECTORY && (!directoryPath
			.toFile().exists() || directory.isEmpty()))
		{
			String message = !directory.isEmpty()
				? "Directory %s for %s does not exist."
				: "Directory for %2$s is not selected.";
			SimpleDialog.showWarning("Invalid input provided", String.format(message,
				directoryPath.toAbsolutePath(), type));
			return false;
		}

		return true;
	}

	private boolean pathPointsToFile(String directory) {
		// In case of a macro or script job type, check if selected directory points
		// to a file as it should be:
		boolean scriptFileHasBeenSelected = new File(directory).isFile();
		if (!workflowSpimRadioButton.isSelected() && !scriptFileHasBeenSelected) {
			SimpleDialog.showWarning("Invalid input provided",
				"Please specify a script file and not a directory.");
			return false;
		}
		return true;
	}

	private void obtainValues() {
		this.inputDataLocation = obtainDataLocation(inputDataLocationToggleGroup);
		this.outputDataLocation = obtainDataLocation(outputDataLocationToggleGroup);
		this.jobType = obtainJobType(jobTypeSelectorToggleGroup);
	}

	private JobType obtainJobType(ToggleGroup group) {
		int backawardOrderOfSelected = group.getToggles().size() - group
			.getToggles().indexOf(group.getSelectedToggle());
		return JobType.values()[JobType.values().length - backawardOrderOfSelected];
	}

	private DataLocation obtainDataLocation(ToggleGroup group) {
		int backawardOrderOfSelected = group.getToggles().size() - group
			.getToggles().indexOf(group.getSelectedToggle());
		return DataLocation.values()[DataLocation.values().length -
			backawardOrderOfSelected];
	}

	private void selected(Toggle n, Parent disableIfNotSelected) {
		ObservableList<Node> children = disableIfNotSelected
			.getChildrenUnmodifiable();
		boolean disabled = (n != disableIfNotSelected);
		for (Node child : children) {
			child.setDisable(disabled);
		}
	}

	private void selectedSpimWorkflow(boolean spimWorkflowIsSelected) {
		if (spimWorkflowIsSelected) {
			numberOfNodesSpinner.getValueFactory().setValue(1);
			numberOfNodesSpinner.setDisable(true);
			numberOfCoresPerNodeSpinner.setDisable(true);
			demoInputDataRadioButton.setDisable(false);
			jobSubdirectoryRadioButton.setDisable(false);
		}
	}

	private void selectedMacro(boolean macroIsSelected) {
		if (macroIsSelected) {
			numberOfNodesSpinner.setDisable(false);
			jobSubdirectoryRadioButton.setDisable(true);
			numberOfCoresPerNodeSpinner.setDisable(false);
			demoInputDataRadioButton.setDisable(true);
			if (demoInputDataRadioButton.isSelected() || jobSubdirectoryRadioButton
				.isSelected())
			{
				inputDataLocationToggleGroup.selectToggle(ownInputRadioButton);
			}
		}
	}

	private void selectedScript(boolean scriptIsSelected) {
		if (scriptIsSelected) {
			numberOfNodesSpinner.setDisable(false);
			jobSubdirectoryRadioButton.setDisable(true);
			numberOfCoresPerNodeSpinner.setDisable(false);
			demoInputDataRadioButton.setDisable(true);
			if (demoInputDataRadioButton.isSelected() || jobSubdirectoryRadioButton
				.isSelected())
			{
				inputDataLocationToggleGroup.selectToggle(ownInputRadioButton);
			}
		}
	}

	@FXML
	private void previewRemoteCommandButton() {
		// Get temporary values from form fields:
		this.jobType = this.obtainJobType(jobTypeSelectorToggleGroup);

		long numberOfNodes = this.getNumberOfNodes();
		long numberOfCoresPerNode = this.getNumberOfCoresPerNode();
		String queueOrPartition = getQueueOrPartition();
		int[] walltime = getWalltime();
		int maxMemoryPerNode = getMaxMemoryPerNode();
		String userScriptName = getUserScriptName();

		Collections.<String> emptyList();

		// Create the preview job submission script:
		String script = sshHpcClient.previewSubmitCommand(numberOfNodes,
			this.jobType, numberOfCoresPerNode, queueOrPartition, walltime,
			maxMemoryPerNode, userScriptName);

		// Display the preview job submission script:
		PreviewSubmitCommandScreenWindow previewSubmitCommandScreenWindow =
			new PreviewSubmitCommandScreenWindow();
		previewSubmitCommandScreenWindow.showDialog(script);
	}

}
