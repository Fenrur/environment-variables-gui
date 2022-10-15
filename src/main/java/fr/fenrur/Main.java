package fr.fenrur;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
//import jfxtras.styles.jmetro.JMetro;
//import jfxtras.styles.jmetro.Style;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {

    public final static List<String> DEFAULT_SOURCED_FILE = List.of(
            "~/.zshrc",
            "~/.zprofile",
            "~/.profile",
            "~/.bashrc",
            "~/.bash_profile",
            "~/.zsh_profile",
            "~/.config/fish/config.fish",
            "/etc/profile",
            "/etc/bash.bashrc",
            "~/.bash_logout"
    );

    private final static List<Pattern> PATTERNS = List.of(
            Pattern.compile("source \"(.*?)\""),
            Pattern.compile("source '(.*?)'"),
            Pattern.compile("source (.*?)")
    );

    public static Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    public static Optional<Matcher> isSourceLine(String line) {
        return PATTERNS.stream()
                .map(pattern -> pattern.matcher(line))
                .filter(Matcher::matches)
                .findFirst();
    }

    public static URL getResource(String resource, Class<?> clazz) {
        return Objects.requireNonNull(clazz.getResource(resource));
    }

    private static void appendAllSourcedFiles(Set<Path> result, List<Path> toCheck) {
        for (Path check : toCheck) {
            if (Files.isRegularFile(check) && Files.isReadable(check)) {
                result.add(check);
                try {
                    Files.lines(check)
                            .map(Main::isSourceLine)
                            .flatMap(Optional::stream)
                            .map(matcher -> Path.of(matcher.group(1)))
                            .forEach(path -> appendAllSourcedFiles(result, List.of(path)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            final Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Environment Variables Gui");
            alert.setContentText("App can't execute on Windows");
            alert.getButtonTypes().add(ButtonType.CLOSE);
            alert.showAndWait();
            return;
        }

        final Set<Path> sourcedFilePaths = new HashSet<>();
        final List<Path> defaultSourceFilesPaths = DEFAULT_SOURCED_FILE.stream()
                .map(filePath -> filePath.replace("~", System.getenv("HOME")))
                .map(Path::of)
                .toList();

        appendAllSourcedFiles(sourcedFilePaths, defaultSourceFilesPaths);

        stage = primaryStage;
        primaryStage.setTitle("Environment Variables");
        final MainController controller = new MainController(Collections.unmodifiableSet(sourcedFilePaths));
        updateStageFromFXML(stage, "main_controller.fxml", controller, isDark -> {
            if (isDark) {
                controller.pane.setStyle("-fx-background-color: #303030");
            } else {
                controller.pane.setStyle("-fx-background-color: #FFFFFF");
            }
        });

        primaryStage.show();
    }

    public static void updateStageFromFXML(Stage stage, String fxmlFileName, Object controller, Consumer<Boolean> isDark) {
        try {
            final Parent parent = FXMLLoader.load(getResource(fxmlFileName, Main.class), null, null, param -> controller);

            final Scene scene = new Scene(parent, -1, -1, true, SceneAntialiasing.BALANCED);
            stage.setScene(scene);
//            final JMetro jMetro = new JMetro();
//            jMetro.setScene(scene);
//
//            Consumer<Boolean> consumer = d -> {
//                isDark.accept(d);
//                jMetro.setStyle(d ? Style.DARK : Style.LIGHT);
//            };
//
//            consumer.accept(true);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setWindowSizing(Stage stage, Region region) {
        stage.setHeight(region.getPrefHeight());
        stage.setWidth(region.getPrefWidth());
        stage.setMinHeight(region.getMinHeight());
        stage.setMinWidth(region.getMinWidth());
    }
}
