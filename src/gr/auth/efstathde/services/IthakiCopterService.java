package gr.auth.efstathde.services;

import gr.auth.efstathde.helpers.LocalCSVFileWriter;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IthakiCopterService {
    private static final Logger LOGGER = Logger.getLogger(IthakiCopterService.class.getSimpleName());
    private static final String COPTER_CODE = "Q0440";
    private static final int COPTER_PORT = 48075;
    private final List<String> receivedMessages;

    public IthakiCopterService() {
        receivedMessages = new ArrayList<>();
    }

    public void communicateWithCopter() throws IOException {
        var rxBuffer = new byte[5000];
        DatagramPacket resPacket = new DatagramPacket(rxBuffer, rxBuffer.length);
        DatagramSocket resSocket = new DatagramSocket(COPTER_PORT);
        DatagramSocket reqSocket = new DatagramSocket();

        for (int i = 0; i < 60; i++){
            try {
                resSocket.receive(resPacket);
                String message = new String(rxBuffer, 0, resPacket.getLength());
                receivedMessages.add(message);
                LOGGER.log(Level.INFO, new String(rxBuffer));
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.toString(), ex);
            }
        }

        resSocket.close();
        reqSocket.close();
        storeData();
    }

    private void storeData() {
        LOGGER.log(Level.INFO, "Writing packets to files.");
        var localFileWriter = new LocalCSVFileWriter();
        try {
            localFileWriter.writeStringsToFile("data/copter_" + COPTER_CODE + "_", receivedMessages,
                    new String[] {"left_motor", "right_motor", "altitude", "temperature", "pressure"});
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
        LOGGER.log(Level.INFO, "Finished writing packets to files.");
    }
}
