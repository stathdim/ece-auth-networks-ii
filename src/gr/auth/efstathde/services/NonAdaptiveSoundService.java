package gr.auth.efstathde.services;

import gr.auth.efstathde.helpers.AudioFileWriter;
import gr.auth.efstathde.helpers.LocalCSVFileWriter;
import gr.auth.efstathde.helpers.SystemConfiguration;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NonAdaptiveSoundService {
    private static final Logger LOGGER = Logger.getLogger(NonAdaptiveSoundService.class.getSimpleName());
    private final String requestCode;
    private final List<String[]> signalFrequencies;
    private final List<String[]> signalSubs;

    public NonAdaptiveSoundService() {
        signalFrequencies = new ArrayList<>();
        signalSubs = new ArrayList<>();
        requestCode = SystemConfiguration.getAudioCode();
    }

    public void getSignals() throws IOException, LineUnavailableException {
        var songRequestCode = requestCode + "F";
        var signalRequestCode = requestCode + "T";

        LOGGER.log(Level.INFO, "Getting song from NonAdaptive service with request Code " + songRequestCode);
        getSignal(songRequestCode, "nonadaptive_song");

        LOGGER.log(Level.INFO, "Getting signal from NonAdaptive service with request Code " + signalRequestCode);
        getSignal(signalRequestCode, "nonadaptive_signal");
    }

    private void getSignal(String requestCode, String filename) throws IOException, LineUnavailableException {
        var ipAddress = SystemConfiguration.getServerIp();
        var serverPort = SystemConfiguration.getServerPort();
        var clientPort = SystemConfiguration.getClientPort();
        int packetCount = 997, b = 2;
        String packetInfo = requestCode + packetCount;

        byte[] txbuffer = packetInfo.getBytes();
        DatagramPacket reqPacket =
                new DatagramPacket(txbuffer, txbuffer.length, InetAddress.getByName(ipAddress), serverPort);
        byte[] rxbuffer = new byte[128];
        DatagramPacket resPacket = new DatagramPacket(rxbuffer, rxbuffer.length);

        DatagramSocket resSocket = new DatagramSocket(clientPort);
        DatagramSocket reqSocket = new DatagramSocket();

        byte[] freqs = new byte[128 * 2 * packetCount];

        reqSocket.send(reqPacket);
        resSocket.setSoTimeout(1000);

        for(int i = 0; i < packetCount; i++){
            try {
                int sub1, sub2;
                resSocket.receive(resPacket);
                for (int j = 0; j < 128; j++){
                    int a = rxbuffer[j];
                    int index = i*256 + 2*j;
                    sub1 = ((a >> 0x4) & 0xF) - 0x8;
                    sub2 = (a & 0xF) - 0x8;
                    freqs[index] = (index == 0) ? (byte) 0 : (byte) (b * sub1 + freqs[index + 1]);
                    freqs[index + 1] = (byte) (b * (sub2) + freqs[index]);

                    signalSubs.add(new String[] {String.valueOf(index), String.valueOf(sub1)});
                    signalSubs.add(new String[] {String.valueOf(index + 1), String.valueOf(sub2)});
                    signalFrequencies.add(new String[] {String.valueOf(index), String.valueOf(freqs[index])});
                    signalFrequencies.add(new String[] {String.valueOf(index + 1), String.valueOf(freqs[index + 1])});
                }
            } catch (Exception ex){
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
            }
        }
        storeData(requestCode);
        storeSoundClip(freqs, requestCode, filename);

        resSocket.close();
        reqSocket.close();
    }

    private static void storeSoundClip(byte[] freqs, String requestCode, String filename) throws IOException {
        AudioFileWriter.storeSoundClip(freqs, requestCode, filename);
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
