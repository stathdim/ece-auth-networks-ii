package gr.auth.efstathde.services;

import gr.auth.efstathde.helpers.AudioFileWriter;
import gr.auth.efstathde.helpers.LocalCSVFileWriter;
import gr.auth.efstathde.helpers.SystemConfiguration;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdaptiveSoundService {
    private static final Logger LOGGER = Logger.getLogger(AdaptiveSoundService.class.getSimpleName());
    private String requestCode;
    private List<String[]> signalSubs;
    private List<String[]> signalSamples;
    private List<String[]> signalMeans;
    private List<String[]> signalBetas;

    public AdaptiveSoundService() {
        signalSamples = new ArrayList<>();
        signalSubs = new ArrayList<>();
        signalMeans = new ArrayList<>();
        signalBetas = new ArrayList<>();

        requestCode = SystemConfiguration.getAudioCode() + "AQF";
    }

    public void getSoundFile() throws IOException, LineUnavailableException {
        LOGGER.log(Level.INFO, "Getting song from Adaptive Sound service with request Code " + requestCode);

        int packetCount = 997;
        var ipAddress = SystemConfiguration.getServerIp();
        var serverPort = SystemConfiguration.getServerPort();
        var clientPort = SystemConfiguration.getClientPort();
        String packetInfo = requestCode + packetCount;

        // Packet spec
        byte[] txbuffer = packetInfo.getBytes();
        DatagramPacket reqPacket =
                new DatagramPacket(txbuffer, txbuffer.length, InetAddress.getByName(ipAddress), serverPort);
        byte[] rxbuffer = new byte[132];
        DatagramPacket resPacket = new DatagramPacket(rxbuffer, rxbuffer.length);
        // Handle sockets
        DatagramSocket resSocket = new DatagramSocket(clientPort);
        DatagramSocket reqSocket = new DatagramSocket();

        byte[] meanB = new byte[4];
        byte[] betta = new byte[4];
        byte sign;
        byte[] freqs = new byte[256 * 2 * packetCount];
        int rx, sub1, sub2, sample1 = 0, sample2 = 0, counter = 4, mean, beta, hint = 0, sumplCount = 0;

        reqSocket.send(reqPacket);
        resSocket.setSoTimeout(1000);

        for (int i = 1; i < packetCount; i++) {
            resSocket.receive(resPacket);
            sign = (byte) ((rxbuffer[1] & 0x80) != 0 ? 0xff : 0x00); //converting byte[2] to integer
            mean = handleSignalMean(rxbuffer, meanB, sign, i);

            sign = (byte) ((rxbuffer[3] & 0x80) != 0 ? 0xff : 0x00);
            beta = handleSignalBeta(rxbuffer, betta, sign, i);

            for (int j = 4; j <= 131; j++) {
                rx = rxbuffer[j];
                sub1 = (rx & 0x0000000F) - 8;
                sub2 = ((rxbuffer[j] & 0x000000F0) >> 4) - 8;
                signalSubs.add(new String[]{String.valueOf(++sumplCount), String.valueOf(sub1)});
                signalSubs.add(new String[]{String.valueOf(++sumplCount), String.valueOf(sub2)});
                sub1 = sub1 * beta;
                sub2 = sub2 * beta;
                sample1 = hint + sub1 + mean;
                sample2 = sub1 + sub2 + mean;
                hint = sub2;
                counter += 4;
                freqs[counter] = (byte) (sample1 & 0x000000FF);
                freqs[counter + 1] = (byte) ((sample1 & 0x0000FF00) >> 8);
                freqs[counter + 2] = (byte) (sample2 & 0x000000FF);
                freqs[counter + 3] = (byte) ((sample2 & 0x0000FF00) >> 8);
                signalSamples.add(new String[]{String.valueOf(counter), String.valueOf(freqs[counter])});
                signalSamples.add(new String[]{String.valueOf(counter + 1), String.valueOf(freqs[counter + 1])});
                signalSamples.add(new String[]{String.valueOf(counter + 2), String.valueOf(freqs[counter + 2])});
                signalSamples.add(new String[]{String.valueOf(counter + 3), String.valueOf(freqs[counter + 3])});
            }
        }
        resSocket.close();
        reqSocket.close();

        storeData();
        storeSoundClip(freqs, requestCode);
        playAudio(packetCount, freqs);
    }

    private static void storeSoundClip(byte[] freqs, String requestCode) throws IOException {
        AudioFileWriter.storeSoundClip(freqs, requestCode, "adaptive_song", 16);
    }

    private void playAudio(int packetCount, byte[] freqs) throws LineUnavailableException {
        AudioFormat FAudio = new AudioFormat(8000, 16, 1, true, false);
        SourceDataLine dl = AudioSystem.getSourceDataLine(FAudio);
        dl.open(FAudio, 32000);
        dl.start();
        dl.write(freqs, 0, 256 * 2 * packetCount);
        dl.stop();
        dl.close();
    }

    private int handleSignalBeta(byte[] rxbuffer, byte[] betta, byte sign, int i) {
        int beta;
        betta[3] = sign;
        betta[2] = sign;
        betta[1] = rxbuffer[3];
        betta[0] = rxbuffer[2];
        beta = ByteBuffer.wrap(betta).order(ByteOrder.LITTLE_ENDIAN).getInt();
        signalBetas.add(new String[]{String.valueOf(i), String.valueOf(beta)});
        return beta;
    }

    private int handleSignalMean(byte[] rxbuffer, byte[] meanB, byte sign, int i) {
        int mean;
        meanB[3] = sign;
        meanB[2] = sign;
        meanB[1] = rxbuffer[1];
        meanB[0] = rxbuffer[0];
        mean = ByteBuffer.wrap(meanB).order(ByteOrder.LITTLE_ENDIAN).getInt();
        signalMeans.add(new String[]{String.valueOf(i), String.valueOf(mean)});
        return mean;
    }

    private void storeData() {
        LOGGER.log(Level.INFO, "Writing signal data to files.");
        var localFileWriter = new LocalCSVFileWriter();
        try {
            localFileWriter.writeToFile("data/AQDPCM_subs" + requestCode + "_", signalSubs
                    , new String[]{"sub", "value"});
            localFileWriter.writeToFile("data/AQDPCM_frequencies_" + requestCode + "_", signalSamples,
                    new String[]{"sample", "value"});
            localFileWriter.writeToFile("data/AQDPCM_betas_" + requestCode + "_", signalBetas,
                    new String[]{"beta", "value"});
            localFileWriter.writeToFile("data/AQDPCM_means_" + requestCode + "_", signalMeans,
                    new String[]{"mean", "value"});
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
        LOGGER.log(Level.INFO, "Finished signal data to files.");
    }
}
