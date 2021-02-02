package gr.auth.efstathde.services;

import gr.auth.efstathde.helpers.LocalCSVFileWriter;
import gr.auth.efstathde.helpers.SystemConfiguration;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NonAdaptiveSoundService {
    private static final Logger LOGGER = Logger.getLogger(NonAdaptiveSoundService.class.getSimpleName());
    private static final String CODE = "A8236";
    private static List<String[]> signalFrequencies;
    private static List<String[]> signalSubs;
    public static final int PACKET_COUNT = 997;
    public static final int DIFFERENCE_MULTIPLIER = 2;

    public NonAdaptiveSoundService() {
        signalFrequencies = new ArrayList<>();
        signalSubs = new ArrayList<>();
    }

    public void getSignals() throws IOException, LineUnavailableException {
        getSignal(CODE + "F");
        signalFrequencies.clear();
        signalSubs.clear();
        getSignal(CODE + "T");
    }

    private void getSignal(String requestCode) throws IOException, LineUnavailableException {
        var ipAddress = SystemConfiguration.getServerIp();
        var serverPort = SystemConfiguration.getServerPort();
        var clientPort = SystemConfiguration.getClientPort();
        String packetInfo = requestCode + PACKET_COUNT;

        byte[] txbuffer = packetInfo.getBytes();
        DatagramPacket reqPacket =
                new DatagramPacket(txbuffer, txbuffer.length, InetAddress.getByName(ipAddress), serverPort);
        byte[] rxbuffer = new byte[128];
        DatagramPacket resPacket = new DatagramPacket(rxbuffer, rxbuffer.length);

        DatagramSocket resSocket = new DatagramSocket(clientPort);
        DatagramSocket reqSocket = new DatagramSocket();

        byte[] freqs = new byte[128 * 2 * PACKET_COUNT];

        reqSocket.send(reqPacket);
        resSocket.setSoTimeout(1000);

        for(int i = 0; i < PACKET_COUNT; i++){
            try {
                resSocket.receive(resPacket);
                processResponse(rxbuffer, freqs, i);
            } catch (Exception ex){
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
            }
        }
        storeData(requestCode);
        storeSoundClip(freqs, CODE, "non_adaptive", 8);
        playSoundClip(PACKET_COUNT, freqs);

        resSocket.close();
        reqSocket.close();
    }

    private void processResponse(byte[] rxbuffer, byte[] freqs, int packetCount) {
        for (int i = 0; i < 128; i++){
            int a = rxbuffer[i];
            int index = packetCount *256 + 2*i;
            int sub1 = maskFirstDifference(a);
            int sub2 = maskSecondDifference(a);
            addToFrequencies(freqs, sub1, sub2, index);
            addValuesToCollector(freqs, new String[]{String.valueOf(index), String.valueOf(sub1)},
                    new String[]{String.valueOf(index + 1), String.valueOf(sub2)}, index);
        }
    }

    private void addValuesToCollector(byte[] freqs, String[] e, String[] e1, int index) {
        signalSubs.add(e);
        signalSubs.add(e1);
        signalFrequencies.add(new String[] {String.valueOf(index), String.valueOf(freqs[index])});
        signalFrequencies.add(new String[] {String.valueOf(index + 1), String.valueOf(freqs[index + 1])});
    }

    private void addToFrequencies(byte[] freqs, int sub1, int sub2, int index) {
        freqs[index] = (index == 0) ? (byte) 0 : (byte) (DIFFERENCE_MULTIPLIER * sub1 + freqs[index + 1]);
        freqs[index + 1] = (byte) (DIFFERENCE_MULTIPLIER * sub2 + freqs[index]);
    }

    private int maskSecondDifference(int a) {
        return (a & 0xF) - 0x8;
    }

    private int maskFirstDifference(int a) {
        return ((a >> 0x4) & 0xF) - 0x8;
    }

    public static void storeSoundClip(byte[] freqs, String requestCode, String filename, int quantBits) throws IOException {
        AudioFormat FAudio = new AudioFormat(8000, quantBits, 1, true, false);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd HH_mm_ss");

        File file = new File("data/" + filename + "_" + requestCode + "_" + formatter.format(LocalDateTime.now()) + ".wav");

        ByteArrayInputStream Audio_Data = new ByteArrayInputStream(freqs);
        AudioInputStream Audio = new AudioInputStream(Audio_Data, FAudio, freqs.length);

        AudioSystem.write(Audio, AudioFileFormat.Type.WAVE, file);
    }

    private void playSoundClip(int packetCount, byte[] freqs) throws LineUnavailableException {
        AudioFormat FAudio = new AudioFormat(8000, 8, 1, true, false);
        SourceDataLine dl = AudioSystem.getSourceDataLine(FAudio);
        System.out.println("Playing sound");
        dl.open(FAudio, 32000);
        dl.start();
        dl.write(freqs, 0, 256 * packetCount);
        dl.stop();
        dl.close();
    }
    private void storeData(String requestCode) {
        LOGGER.log(Level.INFO, "Writing packets to files.");
        var localFileWriter = new LocalCSVFileWriter();
        try {
            localFileWriter.writeToFile("data/DPCM_samples_" + requestCode + "_", signalFrequencies, new String[] {"sample", "value"});
            localFileWriter.writeToFile("data/DPCM_subs_" + requestCode + "_", signalSubs, new String[] {"sub", "value"});
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
        LOGGER.log(Level.INFO, "Finished writing packets to files.");
    }
}
