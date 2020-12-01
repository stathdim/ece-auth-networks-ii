package gr.auth.efstathde.services;
import gr.auth.efstathde.helpers.LocalCSVFileWriter;
import gr.auth.efstathde.helpers.SystemConfiguration;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PacketService {
    private static final Logger LOGGER = Logger.getLogger(PacketService.class.getSimpleName());
    private static final int EXCHANGE_DURATION_MS = 24000;
    private static final String ECHO_REQUEST_CODE = "E2645";
    private final List<String[]> messages;

    public PacketService() {
        this.messages = new ArrayList<>();
    }

    public void getPacketsWithTemperature() throws Exception
    {
        getFromServer(ECHO_REQUEST_CODE + "T00"); // Only station still operational
        storeDataForTemperature();
        messages.clear();
    }

    public void getPacketsWithoutTemperature() throws Exception
    {
        getFromServer(ECHO_REQUEST_CODE);
        storeData("packets_without_temp");
        messages.clear();
    }

    public void getPacketsWithDisabledRandomness() throws Exception
    {
        getFromServer("E0000");
        storeData("packets_without_random");
        messages.clear();
    }


    private void getFromServer(String echoRequestCode) throws Exception
    {
        var clientPort = SystemConfiguration.getClientPort();
        var serverIp = SystemConfiguration.getServerIp();
        var serverPort = SystemConfiguration.getServerPort();

        var txbuffer = echoRequestCode.getBytes();
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
                messages.add(new String[] {receivedMessage, String.valueOf(duration)});
                LOGGER.log(Level.INFO, "Received packet after " + String.format("%.2f", duration / 1000.0) + " second");
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE ,ex.getMessage());
            }
        }
        responseSocket.close();
        requestSocket.close();
    }

    private void storeData(String name) {
        LOGGER.log(Level.INFO, "Writing packets to files.");
        var localFileWriter = new LocalCSVFileWriter();
        try {
            localFileWriter.writeToFile("data/" + name, messages, new String[] {"message", "duration"});
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
        LOGGER.log(Level.INFO, "Finished writing packets to files.");
    }

    private void storeDataForTemperature() {
        LOGGER.log(Level.INFO, "Writing packets with temperature to files.");
        var localFileWriter = new LocalCSVFileWriter();
        try {
            localFileWriter.writeToFile("data/messages_with_temp_" + ECHO_REQUEST_CODE + "_", messages, new String[] {"Message", "Duration"});
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
        LOGGER.log(Level.INFO, "Finished writing packets with temperature to files.");
    }


}
