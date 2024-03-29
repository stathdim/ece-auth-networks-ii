package gr.auth.efstathde.helpers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalCSVFileWriter {
    public void writeToFile(String filename, List<String[]> data, String[] headers) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd HH_mm_ss");
        File file = new File(filename + formatter.format(LocalDateTime.now()) + ".csv");

        data.add(0, headers);

        try (PrintWriter pw = new PrintWriter(file)) {
            data.stream()
                    .map(this::convertToCSV)
                    .forEach(pw::println);
        }
    }

    public void writeStringsToFile(String filename, List<String> data, String[] headers) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd HH_mm_ss");
        File file = new File(filename + formatter.format(LocalDateTime.now()) + ".csv");

        data.add(0, headers.toString());

        try (PrintWriter pw = new PrintWriter(file)) {
            data.stream()
                    .forEach(pw::println);
        }
    }

    public String convertToCSV(String[] data) {
        return Stream.of(data)
                .collect(Collectors.joining(","));
    }
}
