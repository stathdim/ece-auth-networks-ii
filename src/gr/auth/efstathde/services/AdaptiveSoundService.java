package gr.auth.efstathde.services;

import gr.auth.efstathde.helpers.LocalCSVFileWriter;
import gr.auth.efstathde.helpers.SystemConfiguration;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedWriter;
import java.io.FileWriter;
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
    private static final String CODE = "A7081AQF";
    private static List<String[]> signalSubs;
    private static List<String[]> signalSamples;
    private static List<String[]> signalMeans;
    private static List<String[]> signalBetas;

    public AdaptiveSoundService() {
        signalSamples = new ArrayList<>();
        signalSubs = new ArrayList<>();
        signalMeans = new ArrayList<>();
        signalBetas = new ArrayList<>();
    }

    public void test() throws IOException, LineUnavailableException {
        int packetCount = 997;
        String packetInfo = CODE + packetCount;
        System.out.println(packetInfo);

        var ipAddress = SystemConfiguration.getServerIp();
        var serverPort = SystemConfiguration.getServerPort();
        var clientPort = SystemConfiguration.getClientPort();

        // File creation
        String filename = "data/AQDPCM_" + CODE + "_subs.csv";
        BufferedWriter subtr = new BufferedWriter(new FileWriter(filename));
        subtr.write("sub,value");
        subtr.newLine();

        filename = "data/AQDPCM_" + CODE + "_freqs.csv";
        BufferedWriter sampls = new BufferedWriter(new FileWriter(filename));
        sampls.write("sample,value");
        sampls.newLine();

        filename = "data/AQDPCM_" + CODE + "_means.csv";
        BufferedWriter means = new BufferedWriter(new FileWriter(filename));
        means.write("mean,value");
        means.newLine();

        filename = "data/AQDPCM_" + CODE + "_betas.csv";
        BufferedWriter betas = new BufferedWriter(new FileWriter(filename));
        betas.write("beta,value");
        betas.newLine();

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

        for(int i = 1; i < packetCount; i++){
            if (i % 100 == 0) System.out.println(i);
            try{
                resSocket.receive(resPacket);
                sign = (byte)( ( rxbuffer[1] & 0x80) !=0 ? 0xff : 0x00); //converting byte[2] to integer
                meanB[3] = sign;
                meanB[2] = sign;
                meanB[1] = rxbuffer[1];
                meanB[0] = rxbuffer[0];
                mean = ByteBuffer.wrap(meanB).order(ByteOrder.LITTLE_ENDIAN).getInt();
                means.write(String.format("%d,%d\n", i, mean));
                sign = (byte)( ( rxbuffer[3] & 0x80) !=0 ? 0xff : 0x00);
                betta[3] = sign;
                betta[2] = sign;
                betta[1] = rxbuffer[3];
                betta[0] = rxbuffer[2];
                beta = ByteBuffer.wrap(betta).order(ByteOrder.LITTLE_ENDIAN).getInt();
                betas.write(String.format("%d,%d\n", i, beta));
                for (int j = 4;j <= 131; j++){
                    rx = rxbuffer[j];
                    sub1 = (int)(rx & 0x0000000F)-8;
                    sub2 = (int)((rxbuffer[j] & 0x000000F0)>>4)-8;
                    subtr.write(String.format("%d,%d\n%d,%d\n", ++sumplCount, sub1, ++sumplCount, sub2));
                    sub1 = sub1*beta;
                    sub2 = sub2*beta;
                    sample1 = hint + sub1 + mean;
                    sample2 = sub1 + sub2 + mean;
                    hint = sub2;
                    counter += 4;
                    freqs[counter] = (byte)(sample1 & 0x000000FF);
                    freqs[counter + 1] = (byte)((sample1 & 0x0000FF00)>>8);
                    freqs[counter + 2] = (byte)(sample2 & 0x000000FF);
                    freqs[counter + 3] = (byte)((sample2 & 0x0000FF00)>>8);
                    sampls.write(String.format("%d,%d\n%d,%d\n%d,%d\n%d,%d\n",
                            counter, freqs[counter], counter + 1, freqs[counter + 1],
                            counter + 2, freqs[counter + 2], counter + 3, freqs[counter + 3]));
                }
            }catch(Exception x){
                System.out.println(x);
            }
        }

        AudioFormat FAudio = new AudioFormat(8000, 16, 1, true, false);
        SourceDataLine dl = AudioSystem.getSourceDataLine(FAudio);
        dl.open(FAudio,32000);
        dl.start();
        dl.write(freqs, 0, 256*2*packetCount);
        dl.stop();
        dl.close();

        // close connections
        resSocket.close();
        reqSocket.close();
        //handle file streams
        subtr.flush();
        sampls.flush();
        subtr.close();
        sampls.close();
        means.flush();
        means.close();
        betas.flush();
        betas.close();
    }

    public void getSoundFile() throws IOException, LineUnavailableException {
        int packetCount = 997;
        var ipAddress = SystemConfiguration.getServerIp();
        var serverPort = SystemConfiguration.getServerPort();
        var clientPort = SystemConfiguration.getClientPort();
        String packetInfo = CODE + packetCount;

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
        playAudio(packetCount, freqs);
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
            localFileWriter.writeToFile("data/AQDPCM_subs" + CODE + "_", signalSubs
                    , new String[]{"sub", "value"});
            localFileWriter.writeToFile("data/AQDPCM_frequencies_" + CODE + "_", signalSamples,
                    new String[]{"sample", "value"});
            localFileWriter.writeToFile("data/AQDPCM_betas_" + CODE + "_", signalBetas,
                    new String[]{"beta", "value"});
            localFileWriter.writeToFile("data/AQDPCM_means_" + CODE + "_", signalMeans,
                    new String[]{"mean", "value"});
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
        LOGGER.log(Level.INFO, "Finished signal data to files.");
    }
}
