package fr.fenrur;

//import com.jthemedetecor.OsThemeDetector;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

public class MainController implements Initializable {

    public static int ROW = 50;

    private final Set<Path> sourcedFilePaths;

    public GridPane pane;
    public TableView<EnvironmentVariable> tableView;
    public Label label;
    public Button newButton;
    public Button modifyButton;
    public Button deleteButton;
    public TableColumn<EnvironmentVariable, String> variableColumn;
    public TableColumn<EnvironmentVariable, String> valueColumn;
    public TableColumn<EnvironmentVariable, String> fileColumn;
    public RowConstraints row1;
    public RowConstraints row2;
    public RowConstraints row3;

    public MainController(Set<Path> sourcedFilePaths) {
        this.sourcedFilePaths = sourcedFilePaths;
    }

    public void selectFirstItem() {
        if (!tableView.getItems().isEmpty()) {
            tableView.getSelectionModel().select(0);
        }
    }

    public void setTableColumnFactory() {
        variableColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().key()));
        valueColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().toValueLine()));
        fileColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().sourceFilePath().toAbsolutePath().toString()));
    }

    public void addSelectItemListener() {
        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            final boolean disable = !Files.isWritable(newValue.sourceFilePath());
            deleteButton.setDisable(disable);
            modifyButton.setDisable(disable);
            newButton.setDisable(!Files.isWritable(tableView.getItems().get(0).sourceFilePath()));
        });
    }

    public void addResizeListener() {
        Platform.runLater(() -> tableView.widthProperty().addListener((observableValue, oldSceneWidth, newSceneWidth) -> {
            final double factor = newSceneWidth.doubleValue() / oldSceneWidth.doubleValue();
            final double newSizeWidthKeyColumn = variableColumn.getWidth() * factor;
            final double newSizeWidthValueColumn = valueColumn.getWidth() * factor;
            variableColumn.setPrefWidth(newSizeWidthKeyColumn);
            valueColumn.setPrefWidth(newSizeWidthValueColumn);
            fileColumn.setPrefWidth((newSceneWidth.doubleValue() - (newSizeWidthKeyColumn + newSizeWidthValueColumn)) - 5);
        }));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addResizeListener();
        addSelectItemListener();
        sourcedFilePaths.forEach(path -> tableView.getItems().addAll(EnvironmentVariableManager.findVariablesFrom(path)));
        setTableColumnFactory();
        selectFirstItem();
        pane.heightProperty().addListener((observable, oldValue, newValue) -> resizeRows());
        label.setText(label.getText() + " " + (System.getenv("SUDO_USER") != null ? System.getenv("SUDO_USER") : System.getenv("USER")));
        Main.setWindowSizing(Main.stage, pane);
    }

    public void onDeleteButtonClicked(MouseEvent mouseEvent) {
        final EnvironmentVariable environmentVariable = tableView.getSelectionModel().getSelectedItem();
        final boolean isDeleted = EnvironmentVariableManager.deleteVariable(environmentVariable);
        if (isDeleted) {
            tableView.getItems().remove(environmentVariable);
        }
    }

    public void onNewButtonClicked(MouseEvent mouseEvent) throws IOException {
        final EditorVariableController editorVariableController = new EditorVariableController(
                tableView,
                sourcedFilePaths,
                null);

        final Stage stage = new Stage();
        stage.setTitle("New variable");

        Main.updateStageFromFXML(stage, "editor_variable_controller.fxml", editorVariableController, isDark -> {
            if (isDark) {
                editorVariableController.pane.setStyle("-fx-background-color: #303030");
            } else {
                editorVariableController.pane.setStyle("-fx-background-color: #FFFFFF");
            }
        });

        stage.show();
        Main.stage.hide();
    }

    public void onModifyButtonClicked(MouseEvent mouseEvent) throws IOException {
        final EditorVariableController editorVariableController = new EditorVariableController(
                tableView,
                sourcedFilePaths,
                tableView.getSelectionModel().getSelectedItem()
        );
        final Stage stage = new Stage();
        stage.setTitle("Modify variable");

        Main.updateStageFromFXML(stage, "editor_variable_controller.fxml", editorVariableController, isDark -> {
            if (isDark) {
                editorVariableController.pane.setStyle("-fx-background-color: #303030");
            } else {
                editorVariableController.pane.setStyle("-fx-background-color: #FFFFFF");
            }
        });

        stage.show();
        Main.stage.hide();
    }

    public void resizeRows() {
        row2.setPrefHeight(pane.getHeight() - ROW - ROW);
        row1.setPrefHeight(ROW);
        row3.setPrefHeight(ROW);
    }
}
