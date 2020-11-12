package gr.auth.efstathde.helpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.*;

public class LocalFileWriter {
    public static void writeToFile(String filename, List<String> data) throws IOException {
        Files.write(Paths.get(filename), data, CREATE, WRITE, TRUNCATE_EXISTING);
    }

    public static void writeLongsToFile(String filename, List<Long> data) throws IOException {
        var stringifiedData = data.stream().map(String::valueOf).collect(Collectors.toList());
        Files.write(Paths.get(filename), stringifiedData, CREATE, WRITE, TRUNCATE_EXISTING);
    }
}
