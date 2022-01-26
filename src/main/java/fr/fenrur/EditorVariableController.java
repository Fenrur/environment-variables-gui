package fr.fenrur;

import javafx.application.Platform;
import javafx.beans.value.ObservableValueBase;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

public class EditorVariableController implements Initializable {

    public static int COLUMN = 145;
    public static int ROW = 60;
    private final TableView<EnvironmentVariable> mainTableView;
    private final Set<Path> sourcedFilePaths;
    private final Optional<EnvironmentVariable> from;

    public GridPane pane;
    public Button browseDirectoryButton;
    public Button browseFileButton;
    public Button okButton;
    public Button cancelButton;
    public ChoiceBox<Path> fileChoiceBox;
    public RowConstraints row1;
    public RowConstraints row2;
    public RowConstraints row3;
    public RowConstraints row4;
    public ColumnConstraints column1;
    public ColumnConstraints column2;
    public TableColumn<StringBuilder, TextField> tableColumn;
    public TableView<StringBuilder> tableView;
    public Button removeButton;
    public TextField keyTextField;
    public Button addButton;

    private StringBuilder selected;
    private DirectoryChooser directoryChooser;
    private FileChooser fileChooser;

    public EditorVariableController(TableView<EnvironmentVariable> mainTableView, Set<Path> sourcedFilePaths, Optional<EnvironmentVariable> from) {
        this.mainTableView = mainTableView;
        this.sourcedFilePaths = sourcedFilePaths;
        this.from = from;
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            onMouseClickedCancelButton(null);
        } else if (keyEvent.getCode() == KeyCode.ENTER && !okButton.isDisable()) {
            onMouseClickedOkButton(null);
        }
    }

    public void onMouseClickedOkButton(MouseEvent mouseEvent) {
        if (verifyDoubleQuotesAreEscapeAndShowAlertIfNot(keyTextField.getText())) return;

        for (StringBuilder item : tableView.getItems()) {
            if (verifyDoubleQuotesAreEscapeAndShowAlertIfNot(item.toString())) return;
        }

        final Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();

        if (from.isPresent()) {
            final boolean deleted = EnvironmentVariableManager.deleteVariable(from.get());
            if (!deleted) {
                Main.stage.show();
            } else {
                mainTableView.getItems().remove(from.get());
            }
        }

        final EnvironmentVariable variable = new EnvironmentVariable(
                fileChoiceBox.getSelectionModel().getSelectedItem(),
                keyTextField.getText(),
                tableView.getItems().stream().map(StringBuilder::toString).filter(s -> !s.isBlank()).toList()
        );
        if (EnvironmentVariableManager.writeVariable(variable)) {
            mainTableView.getItems().add(variable);
            mainTableView.refresh();
        }

        Main.stage.show();
    }

    private boolean verifyDoubleQuotesAreEscapeAndShowAlertIfNot(String value) {
        final char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '"' && (i == 0 || chars[i - 1] != '\\')) {
                final Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Environment Variables Gui");
                alert.setContentText("The values: " + value + " need to escape (\") with (\\) before");
                alert.getButtonTypes().add(ButtonType.CLOSE);
                alert.showAndWait();
                return true;
            }
        }
        return false;
    }

    public void onMouseClickedCancelButton(MouseEvent mouseEvent) {
        Main.stage.show();
        final Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }

    public void onMouseClickedBrowseDirectoryButton(MouseEvent mouseEvent) {
        if (directoryChooser == null) {
            directoryChooser = new DirectoryChooser();
        }
        final File file = directoryChooser.showDialog(getStage());
        if (file == null) return;
        tableColumn.getCellObservableValue(selected).getValue().setText(file.getAbsolutePath());
        tableView.refresh();
    }

    public void onMouseClickedBrowseFileButton(MouseEvent mouseEvent) {
        if (fileChooser == null) {
            fileChooser = new FileChooser();
        }
        final File file = fileChooser.showOpenDialog(getStage());
        if (file == null) return;
        tableColumn.getCellObservableValue(selected).getValue().setText(file.getAbsolutePath());
        tableView.refresh();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        keyTextField.textProperty().addListener((observable, oldValue, newValue) -> refreshDisableButtons());

        tableColumn.setCellValueFactory(param -> new ObservableValueBase<>() {
            @Override
            public TextField getValue() {
                final TextField textField = new TextField(param.getValue().toString());
                textField.textProperty().addListener((observable, oldValue, newValue) -> {
                    param.getValue()
                            .delete(0, param.getValue().length())
                            .append(newValue);
                });
                textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                    selected = param.getValue();
                    refreshDisableButtons();
                });
                textField.onMouseClickedProperty().addListener((observable, oldValue, newValue) -> {
                    selected = param.getValue();
                    refreshDisableButtons();
                });
                return textField;
            }
        });
        resizeRows();
        resizeColumns();
        pane.widthProperty().addListener((observable, oldValue, newValue) -> {
            resizeColumns();
            resizeTableColumn();
        });
        pane.heightProperty().addListener((observable, oldValue, newValue) -> resizeRows());
        Platform.runLater(this::resizeTableColumn);

        fileChoiceBox.getItems().addAll(sourcedFilePaths);
        fileChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshDisableButtons());

        if (from.isPresent()) {
            final EnvironmentVariable environmentVariable = from.get();
            keyTextField.setText(environmentVariable.key());
            tableView.getItems().addAll(environmentVariable.values().stream().map(StringBuilder::new).toList());
            fileChoiceBox.setValue(environmentVariable.sourceFilePath());
        }

        refreshDisableButtons();
    }

    private void refreshDisableButtons() {
        okButton.setDisable(tableView.getItems().isEmpty() || keyTextField.getText() == null || keyTextField.getText().isBlank() || selected == null || fileChoiceBox.getValue() == null);

        if (tableView.getItems().isEmpty()) {
            selected = null;
        }

        if (selected == null) {
            browseDirectoryButton.setDisable(true);
            browseFileButton.setDisable(true);
            removeButton.setDisable(true);
        } else {
            browseDirectoryButton.setDisable(false);
            browseFileButton.setDisable(false);
            removeButton.setDisable(false);
        }
    }

    private void resizeTableColumn() {
        tableColumn.setPrefWidth(tableColumn.getTableView().getWidth() - 3);
    }

    private void resizeColumns() {
        column1.setPrefWidth(COLUMN);
        column2.setPrefWidth(pane.getWidth() - COLUMN);
    }

    public void resizeRows() {
        row2.setPrefHeight(pane.getHeight() - ROW - ROW - ROW);
        row1.setPrefHeight(ROW);
        row3.setPrefHeight(ROW);
        row4.setPrefHeight(ROW);
    }

    public void onMouseClickedAddButton(MouseEvent mouseEvent) {
        tableView.getItems().add(new StringBuilder());
        Platform.runLater(this::refreshDisableButtons);
    }

    public void onMouseClickedRemoveButton(MouseEvent mouseEvent) {
        if (selected != null) {
            tableView.getItems().remove(selected);
        }
        refreshDisableButtons();
        tableView.refresh();
    }

    public Stage getStage() {
        return (Stage) okButton.getScene().getWindow();
    }
}
