package fr.fenrur;

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

public class Controller implements Initializable {

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

    public Controller(Set<Path> sourcedFilePaths) {
        this.sourcedFilePaths = sourcedFilePaths;
    }

    public static void selectFirstItem(TableView<EnvironmentVariable> tableView) {
        if (!tableView.getItems().isEmpty()) {
            tableView.getSelectionModel().select(0);
        }
    }

    public static void setTableColumnFactory(TableColumn<EnvironmentVariable, String> columnVariable, TableColumn<EnvironmentVariable, String> columnValue, TableColumn<EnvironmentVariable, String> columnFile) {
        columnVariable.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().key()));
        columnValue.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().toValueLine()));
        columnFile.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().sourceFilePath().toAbsolutePath().toString()));
    }

    public static void addSelectItemListener(TableView<EnvironmentVariable> tableView, Button deleteButton, Button modifyButton, Button newButton) {
        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            final boolean disable = !Files.isWritable(newValue.sourceFilePath());
            deleteButton.setDisable(disable);
            modifyButton.setDisable(disable);
            newButton.setDisable(!Files.isWritable(tableView.getItems().get(0).sourceFilePath()));
            System.out.println(newValue);
        });
    }

    public static void addResizeListener(TableView<EnvironmentVariable> tableView, TableColumn<EnvironmentVariable, String> variableColumn, TableColumn<EnvironmentVariable, String> valueColumn, TableColumn<EnvironmentVariable, String> fileColumn) {
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
        addResizeListener(tableView, variableColumn, valueColumn, fileColumn);
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        addSelectItemListener(tableView, deleteButton, modifyButton, newButton);
        sourcedFilePaths.forEach(path -> tableView.getItems().addAll(EnvironmentVariableManager.findVariablesFrom(path)));
        setTableColumnFactory(variableColumn, valueColumn, fileColumn);
        selectFirstItem(tableView);
        pane.heightProperty().addListener((observable, oldValue, newValue) -> resizeRows());
        label.setText(label.getText() + " " + (System.getenv("SUDO_USER") != null ? System.getenv("SUDO_USER") : System.getenv("USER")));
    }

    public void onDeleteButtonClicked(MouseEvent mouseEvent) {
        final EnvironmentVariable environmentVariable = tableView.getSelectionModel().getSelectedItem();
        final boolean isDeleted = EnvironmentVariableManager.deleteVariable(environmentVariable);
        if (isDeleted) {
            tableView.getItems().remove(environmentVariable);
        }
    }

    public void onNewButtonClicked(MouseEvent mouseEvent) throws IOException {
        final FXMLLoader loader = new FXMLLoader(Main.getResource("editorVariableController.fxml", getClass()));

        final EditorVariableController editorVariableController = new EditorVariableController(
                tableView,
                sourcedFilePaths,
                Optional.empty());
        loader.setControllerFactory(param -> editorVariableController);
        final Scene scene = new Scene(loader.load());
        final Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("New variable");
        Main.stage.hide();
        stage.show();
        Main.setWindowSizing(stage, editorVariableController.pane);
    }

    public void onModifyButtonClicked(MouseEvent mouseEvent) throws IOException {
        final FXMLLoader loader = new FXMLLoader(Main.getResource("editorVariableController.fxml", getClass()));

        final EditorVariableController editorVariableController = new EditorVariableController(
                tableView,
                sourcedFilePaths,
                Optional.ofNullable(tableView.getSelectionModel().getSelectedItem())
        );
        loader.setControllerFactory(param -> editorVariableController);
        final Scene scene = new Scene(loader.load());
        final Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle("Modify variable");
        Main.stage.hide();
        stage.show();
        Main.setWindowSizing(stage, editorVariableController.pane);
    }

    public void resizeRows() {
        row2.setPrefHeight(pane.getHeight() - ROW - ROW);
        row1.setPrefHeight(ROW);
        row3.setPrefHeight(ROW);
    }
}
