package gr.auth.efstathde.services;

import gr.auth.efstathde.helpers.SystemConfiguration;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IthakiCopterService {
    private static final Logger LOGGER = Logger.getLogger(IthakiCopterService.class.getSimpleName());
    private static final String COPTER_CODE = "Q5891";
    private static final int COPTER_PORT = 38048;
    private String receivedMessage = "";

    public void communicateWithCopter() throws IOException
    {
        communicate(180, 160, 140);
        writeData("data/CopterTelemetry_0"+ COPTER_CODE + ".csv");
        receivedMessage = "";
        communicate(240, 170, 170);
        writeData("data/CopterTelemetry_1"+ COPTER_CODE + ".csv");
    }

    private void communicate(int level, int left, int right) throws IOException {
        InetAddress hostAddress = InetAddress.getByName(SystemConfiguration.getServerIp());
        Socket OutputSocket = new Socket(hostAddress, COPTER_PORT);
        BufferedReader Input = new BufferedReader(new InputStreamReader(OutputSocket.getInputStream()));
        DataOutputStream Output = new DataOutputStream(OutputSocket.getOutputStream());

        try {
            for(int times = 0; times < 300; times++) {
                readTelemetryFromSocket(level, left, right, Input, Output);
            }
        } catch (Exception ex){
            LOGGER.log(Level.SEVERE, ex.getMessage());
        }
    }

    private void writeData(String name) throws FileNotFoundException {
        File DataFile = new File(name);
        FileOutputStream copter_stream = new FileOutputStream(DataFile);
        try {
            copter_stream.write(receivedMessage.getBytes());
            copter_stream.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE,"Error: Could not save copter Results.");
        }
    }

    private void readTelemetryFromSocket(int level, int left, int right, BufferedReader input, DataOutputStream output) throws IOException {
        String message = input.readLine();
        output.writeBytes("AUTO FLIGHTLEVEL=" + level + " LMOTOR=" + left + " RMOTOR=" + right + " PILOT \r\n");
        if (message.contains("ITHAKICOPTER")) {
             DeconstructMessage(message);
        }
    }

    private void DeconstructMessage(String telemetry) {
        var left_motor = telemetry.substring(20, 23);
        var right_motor = telemetry.substring(31, 34);
        var altitude = telemetry.substring(44, 47);
        var temperature = telemetry.substring(60, 66);
        var pressure = telemetry.substring(76, 83);
        this.receivedMessage += left_motor + "," + right_motor + "," + altitude + "," + temperature + "," + pressure + "\r\n";
    }
}
