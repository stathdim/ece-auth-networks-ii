package gr.auth.efstathde.helpers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalFileWriter {
    public void writeToFile(String filename, List<String[]> data, String[] headers) throws IOException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd HH_mm_ss");
        File file = new File(filename + dtf.format(LocalDateTime.now()) + ".csv");

        data.add(0, headers);

        try (PrintWriter pw = new PrintWriter(file)) {
            data.stream()
                    .map(this::convertToCSV)
                    .forEach(pw::println);
        }
    }

    public String convertToCSV(String[] data) {
        return Stream.of(data)
                .collect(Collectors.joining(" , "));
    }
}
