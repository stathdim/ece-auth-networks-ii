package gr.auth.efstathde.services;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.*;

public class PackageService {
    private static final Logger LOGGER = Logger.getLogger(PackageService.class.getSimpleName());
    private static final int EXCHANGE_DURATION_MS = 15000;
    private static final String SERVER_IP = "155.207.18.208";
    private static final String ECHO_REQUEST_CODE = "E8211";
    private static final int serverPort = 38015;
    private static final int clientPort = 48015;
    private final List<String> messages;
    private final List<Long> pingDurations;

    public PackageService() {
        this.messages = new ArrayList<>();
        this.pingDurations = new ArrayList<>();
    }

    public void performPing() throws Exception
    {
        var txbuffer = ECHO_REQUEST_CODE.getBytes();
        var hostAddress = InetAddress.getByName(SERVER_IP);
        var requestSocket = new DatagramSocket();
        var responseSocket = new DatagramSocket(clientPort);
        responseSocket.setSoTimeout(2400);
        var rxbuffer = new byte[2048];
        var requestPacket = new DatagramPacket(txbuffer, txbuffer.length, hostAddress, serverPort);

        var Begin = System.currentTimeMillis();

        while (System.currentTimeMillis() - Begin < EXCHANGE_DURATION_MS) { //3000 for T00
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
            writeToFile("data/messages.txt", messages);
            writeToFile("data/durations.txt", pingDurations.stream().map(String::valueOf).collect(Collectors.toList()));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
        LOGGER.log(Level.INFO, "Finished writing ping data to files.");
    }

    private void writeToFile(String filename, List<String> data) throws IOException {
        Files.write(Paths.get(filename), data, CREATE, WRITE, TRUNCATE_EXISTING);
    }
}
