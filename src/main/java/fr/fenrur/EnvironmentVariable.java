package fr.fenrur;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record EnvironmentVariable(Path sourceFilePath, String key, List<String> values) {

    public EnvironmentVariable {
        Objects.requireNonNull(sourceFilePath);
        Objects.requireNonNull(key);
        Objects.requireNonNull(values);
    }

    public String toLineFile() {
        return '"' + key + '"' + "=\"" + toValueLine() + "\"";
    }

    public String toValueLine() {
        return String.join(":", values);
    }
}
