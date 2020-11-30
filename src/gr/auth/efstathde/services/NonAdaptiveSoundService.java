package gr.auth.efstathde.services;

import gr.auth.efstathde.helpers.LocalCSVFileWriter;
import gr.auth.efstathde.helpers.SystemConfiguration;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
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
    private static final String CODE = "A2524F";
    private static List<String[]> signalFrequencies;
    private static List<String[]> signalSubs;

    public NonAdaptiveSoundService() {
        signalFrequencies = new ArrayList<>();
        signalSubs = new ArrayList<>();
    }

    public void getSignal() throws IOException, LineUnavailableException {
        var ipAddress = SystemConfiguration.getServerIp();
        var serverPort = SystemConfiguration.getServerPort();
        var clientPort = SystemConfiguration.getClientPort();
        int packetCount = 997, b = 2;
        String packetInfo = CODE + packetCount;

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
        storeData();
        playSoundClip(packetCount, freqs);

        resSocket.close();
        reqSocket.close();
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
    private void storeData() {
        LOGGER.log(Level.INFO, "Writing packets to files.");
        var localFileWriter = new LocalCSVFileWriter();
        try {
            localFileWriter.writeToFile("data/DPCM_samples_" + CODE + "_", signalFrequencies, new String[] {"sample", "value"});
            localFileWriter.writeToFile("data/DPCM_subs_" + CODE + "_", signalSubs, new String[] {"sub", "value"});
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
        LOGGER.log(Level.INFO, "Finished writing packets to files.");
    }
}
