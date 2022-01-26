package fr.fenrur;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {

    private final static List<String> DEFAULT_SOURCED_FILE = List.of(
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

    public static boolean runByIntellij = false;
    public static Stage stage;

    public static void main(String[] args) {
        runByIntellij = Arrays.stream(args).anyMatch(arg -> arg.equalsIgnoreCase("--runByIntellij"));
        Application.launch(args);
    }

    public static Optional<Matcher> isSourceLine(String line) {
        return PATTERNS.stream()
                .map(pattern -> pattern.matcher(line))
                .filter(Matcher::matches)
                .findFirst();
    }

    public static URL getResource(String resource, Class<?> clazz) {
        if (runByIntellij) {
            try {
                return Paths.get("src/main/resources/", clazz.getPackageName().replace(".", "/"), resource).toUri().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return null;
        } else {
            return clazz.getResource(resource);
        }
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
    public void start(Stage primaryStage) throws IOException {
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
        final FXMLLoader loader = new FXMLLoader(getResource("controller.fxml", getClass()));
        final Controller controller = new Controller(Collections.unmodifiableSet(sourcedFilePaths));
        loader.setControllerFactory(param -> controller);
        primaryStage.setScene(new Scene(loader.load()));

        primaryStage.show();
        setWindowSizing(stage, controller.pane);
    }

    public static void setWindowSizing(Stage stage, Region region) {
        stage.setHeight(region.getPrefHeight());
        stage.setWidth(region.getPrefWidth());
        stage.setMinHeight(region.getMinHeight());
        stage.setMinWidth(region.getMinWidth());
    }
}
