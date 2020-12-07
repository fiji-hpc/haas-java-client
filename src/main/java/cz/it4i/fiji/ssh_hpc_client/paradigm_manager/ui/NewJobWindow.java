
package cz.it4i.fiji.ssh_hpc_client.paradigm_manager.ui;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.kordamp.ikonli.materialdesign.MaterialDesign;
import org.scijava.plugin.Plugin;

import cz.it4i.cluster_job_launcher.SshJobSettings;
import cz.it4i.fiji.heappe_hpc_client.paradigm_manager.ui.ConnectionType;
import cz.it4i.fiji.heappe_hpc_client.paradigm_manager.ui.NewJobController;
import cz.it4i.fiji.hpc_workflow.core.JobType;
import cz.it4i.fiji.hpc_workflow.ui.IconHelperMethods;
import cz.it4i.fiji.hpc_workflow.ui.JavaFXJobSettingsProvider;
import cz.it4i.fiji.ssh_hpc_client.SshJobSettingsBuilder;
import cz.it4i.fiji.ssh_hpc_client.paradigm_manager.SshClientJobSettings;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;

@Plugin(type = JavaFXJobSettingsProvider.class)
public class NewJobWindow implements
	JavaFXJobSettingsProvider<SshClientJobSettings>
{

	@Override
	public Class<SshClientJobSettings> getTypeOfJobSettings() {
		return SshClientJobSettings.class;
	}

	@Override
	public void provideJobSettings(Window parent,
		Consumer<SshClientJobSettings> consumer)
	{
		final NewJobController controller = new NewJobController(
			ConnectionType.SSH);
		controller.setCreatePressedNotifier(() -> consumer.accept(constructSettings(
			controller)));
		final Scene formScene = new Scene(controller);
		Stage stage = new Stage();
		stage.initOwner(parent);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setResizable(false);
		stage.setTitle("Create job");
		stage.setScene(formScene);
		Image myImage = IconHelperMethods.convertIkonToImage(
			MaterialDesign.MDI_CREATION);
		stage.getIcons().add(myImage);
		finalizeOnStageClose(controller, stage);
		controller.init(stage);
		stage.showAndWait();
	}

	private static SshClientJobSettings constructSettings(
		NewJobController newJobController)
	{
		// ToDo: complete this:
		SshJobSettings sshJobSetttings = new SshJobSettingsBuilder()
				.numberOfCoresPerNode(newJobController.getNumberOfCoresPerNode())
				.numberOfNodes(newJobController.getNumberOfNodes())
				.queueOrPartition(newJobController.getQueueOrPartition()).walltime(newJobController.getWalltime())
				.userScriptName(newJobController.getUserScriptName()).build();

		return new JobWithDirectorySettingsAdapter(sshJobSetttings) {

			private static final long serialVersionUID = 5998838289289128870L;

			@Override
			public String getUserScriptName() {
				return this.jobSettings.getUserScriptName();
			}

			@Override
			public UnaryOperator<Path> getOutputPath() {
				return newJobController::getOutputDirectory;
			}

			@Override
			public UnaryOperator<Path> getInputPath() {
				return newJobController::getInputDirectory;
			}

			@Override
			public JobType getJobType() {
				return newJobController.getJobType();
			}

			@Override
			public String getJobName() {
				return "job name";
			}
		};

	}

	private static void finalizeOnStageClose(NewJobController controller,
		Stage stage)
	{
		stage.setOnCloseRequest((WindowEvent we) -> controller.close());
	}

	@AllArgsConstructor
	private abstract static class JobWithDirectorySettingsAdapter implements
		SshClientJobSettings
	{

		private static final long serialVersionUID = 7219177839749763140L;

		@Delegate(types = SshJobSettings.class)
		protected final SshJobSettings jobSettings;
	}
}
