package gr.auth.efstathde.helpers;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AudioFileWriter {

    public static void storeSoundClip(byte[] freqs, String requestCode, String filename, int quantBits) throws IOException {
        AudioFormat FAudio = new AudioFormat(8000, quantBits, 1, true, false);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd HH_mm_ss");

        File file = new File("data/" + filename + "_" + requestCode + "_" + formatter.format(LocalDateTime.now()) + ".wav");

        ByteArrayInputStream Audio_Data = new ByteArrayInputStream(freqs);
        AudioInputStream Audio = new AudioInputStream(Audio_Data, FAudio, freqs.length);

        AudioSystem.write(Audio, AudioFileFormat.Type.WAVE, file);
    }
}
