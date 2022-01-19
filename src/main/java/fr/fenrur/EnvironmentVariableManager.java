package fr.fenrur;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EnvironmentVariableManager {

    public static final List<Pattern> PATTERNS = List.of(
            Pattern.compile("export \"(.*?)\"=\"(.*?)\""),
            Pattern.compile("export '(.*?)'='(.*?)'"),
            Pattern.compile("export (.*?)=\"(.*?)\""),
            Pattern.compile("export (.*?)='(.*?)'"),
            Pattern.compile("export '(.*?)'=(.*?)"),
            Pattern.compile("export \"(.*?)\"=(.*?)"),
            Pattern.compile("export \"(.*?)\"='(.*?)'"),
            Pattern.compile("export '(.*?)'=\"(.*?)\""),
            Pattern.compile("export (.*?)=(.*?)")
    );

    public static Optional<Matcher> isExportLine(String line) {
        return PATTERNS.stream()
                .map(pattern -> pattern.matcher(line))
                .filter(Matcher::matches)
                .findFirst();
    }

    public static Set<EnvironmentVariable> findVariablesFrom(Path path) {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) return Set.of();
        try {
            return Files.lines(path)
                    .map(EnvironmentVariableManager::isExportLine)
                    .flatMap(Optional::stream)
                    .map(matcher -> new EnvironmentVariable(path, matcher.group(1), List.of(matcher.group(2).split(":"))))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Set.of();
    }

    public static boolean deleteVariable(EnvironmentVariable variable) {
        try {
            final List<String> lines = Files.readAllLines(variable.sourceFilePath());
            final Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                final String next = iterator.next();
                final Optional<Matcher> exportLine = isExportLine(next);
                if (exportLine.isPresent()) {
                    final Matcher matcher = exportLine.get();
                    if (matcher.group(1).equals(variable.key()) && matcher.group(2).equals(variable.toValueLine())) {
                        iterator.remove();
                        break;
                    }
                }
            }
            Files.write(variable.sourceFilePath(), lines);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean writeVariable(EnvironmentVariable variable) {
        try {
            Files.writeString(variable.sourceFilePath(), "\nexport " + variable.toLineFile(), StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
