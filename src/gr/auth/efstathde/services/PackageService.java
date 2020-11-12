package gr.auth.efstathde.services;
import gr.auth.efstathde.helpers.LocalFileWriter;
import gr.auth.efstathde.helpers.SystemConfiguration;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PackageService {
    private static final Logger LOGGER = Logger.getLogger(PackageService.class.getSimpleName());
    private static final int EXCHANGE_DURATION_MS = 15000;
    private static final String ECHO_REQUEST_CODE = "E8211";
    private final List<String> messages;
    private final List<Long> pingDurations;

    public PackageService() {
        this.messages = new ArrayList<>();
        this.pingDurations = new ArrayList<>();
    }

    public void performPing() throws Exception
    {
        var clientPort = SystemConfiguration.getClientPort();
        var serverIp = SystemConfiguration.getServerIp();
        var serverPort = SystemConfiguration.getServerPort();

        var txbuffer = ECHO_REQUEST_CODE.getBytes();
        var hostAddress = InetAddress.getByName(serverIp);
        var requestSocket = new DatagramSocket();
        var responseSocket = new DatagramSocket(clientPort);
        responseSocket.setSoTimeout(2400);
        var rxbuffer = new byte[2048];
        var requestPacket = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);

        var Begin = System.currentTimeMillis();

        while (System.currentTimeMillis() - Begin < EXCHANGE_DURATION_MS) {
            var transmissionStart = System.currentTimeMillis();
            requestSocket.send(requestPacket);
            var responsePacket = new DatagramPacket(rxbuffer, rxbuffer.length);
            try {
                responseSocket.receive(responsePacket);
                var transmissionCompleted = System.currentTimeMillis();
                var receivedMessage = new String(rxbuffer, 0, responsePacket.getLength());
                var duration = transmissionCompleted - transmissionStart;
                pingDurations.add(duration) ;
                messages.add(receivedMessage);
                LOGGER.log(Level.INFO, "Received packet after " + String.format("%.2f", duration / 1000.0) + " second");
            } catch (Exception x) {
                LOGGER.log(Level.SEVERE ,x.getMessage());
            }
        }
        storeData();
    }

    private void storeData() {
        LOGGER.log(Level.INFO, "Writing ping data to files.");
        try {
            LocalFileWriter.writeToFile("data/messages.txt", messages);
            LocalFileWriter.writeLongsToFile("data/durations.txt", pingDurations);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
        LOGGER.log(Level.INFO, "Finished writing ping data to files.");
    }


}
