package gr.auth.efstathde.services;

import gr.auth.efstathde.helpers.SystemConfiguration;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageService {
    private static final Logger LOGGER = Logger.getLogger(ImageService.class.getSimpleName());
    private String requestCode;
    private final List<byte[]> receivedBytes;

    public ImageService() {
        receivedBytes = new ArrayList<>();
        requestCode = SystemConfiguration.getImageCode() + "CAM=";
    }

    public void getImage() throws Exception {
//        getImageFromServer(requestCode + "PTZ");
//        writeToFile(requestCode + "PTZ");
        getImageFromServer(requestCode + "FIX");
        writeToFile(requestCode + "FIX");
    }

    private void getImageFromServer(String requestCode) throws Exception {
        LOGGER.log(Level.INFO ,"Getting image with request code: " + requestCode);
        var clientPort = SystemConfiguration.getClientPort();
        var serverIp = SystemConfiguration.getServerIp();
        var serverPort = SystemConfiguration.getServerPort();

        var txBuffer = requestCode.getBytes();
        var hostAddress = InetAddress.getByName(serverIp);
        var requestSocket = new DatagramSocket();
        var responseSocket = new DatagramSocket(clientPort);
        responseSocket.setSoTimeout(2400);
        var rxBuffer = new byte[2048];
        var requestPacket = new DatagramPacket(txBuffer, txBuffer.length, hostAddress, serverPort);
        var responsePacket = new DatagramPacket(rxBuffer, rxBuffer.length);

        requestSocket.send(requestPacket);

        while (true) {
            try {
                responseSocket.receive(responsePacket);
                receivedBytes.add(rxBuffer.clone());
            } catch (IOException ex) {
                LOGGER.log(Level.INFO, ex.getMessage());
                break;
            }
        }
        LOGGER.log(Level.INFO ,"Successfully read image with request code: " + requestCode);
        responseSocket.close();
        requestSocket.close();
    }

    private void writeToFile(String requestCode) throws IOException {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd HH_mm_ss");
        var timestamp = dateFormatter.format(LocalDateTime.now());

        String filename = "data/img" + requestCode + timestamp + ".jpg";
        OutputStream image = new FileOutputStream(filename);

        receivedBytes.forEach(b -> {
            try {
                image.write(b, 0, 128);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE ,"Unable to write image with request code: " + requestCode);
            }
        });
        receivedBytes.clear();
    }
}
