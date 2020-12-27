package gr.auth.efstathde.services;

import gr.auth.efstathde.helpers.LocalCSVFileWriter;
import gr.auth.efstathde.helpers.SystemConfiguration;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IthakiCopterService {
    private static final Logger LOGGER = Logger.getLogger(IthakiCopterService.class.getSimpleName());
    private String requestCode;
    private static final int COPTER_PORT = 38048;
    private final List<String> receivedMessages;

    public IthakiCopterService() {
        receivedMessages = new ArrayList<>();
        requestCode = SystemConfiguration.getCopterCode();
    }

    public void communicateWithCopter() throws IOException
    {
        LOGGER.log(Level.INFO, "Communicating with copter, code: " + requestCode);
        communicate(150, 150, 150, "data/CopterTelemetry_0"+ requestCode + ".csv");
        communicate(240, 170, 170,"data/CopterTelemetry_1"+ requestCode + ".csv");
    }

    private void communicate(int level, int left, int right, String name) throws IOException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        System.out.println(dtf.format(LocalDateTime.now()));
        File CopterTelemetry = new File(name);
        FileOutputStream copter_stream = new FileOutputStream(CopterTelemetry);

        InetAddress hostAddress = InetAddress.getByName(SystemConfiguration.getServerIp());
        Socket OutputSocket = new Socket(hostAddress, COPTER_PORT);
        BufferedReader Input = new BufferedReader(new InputStreamReader(OutputSocket.getInputStream()));
        DataOutputStream Output = new DataOutputStream(OutputSocket.getOutputStream());

        String telemetry, TelemetryOutput = "";
        String LLL, RRR, AAA, TTTT, PPPP;
        try {
            for(int times = 0; times < 300; times++) {
                telemetry = Input.readLine();
                Output.writeBytes("AUTO FLIGHTLEVEL=" + level + " LMOTOR=" + left + " RMOTOR=" + right + " PILOT \r\n");
                if (telemetry.contains("ITHAKICOPTER")) {
                    LLL = telemetry.substring(20, 23);
                    RRR = telemetry.substring(31, 34);
                    AAA = telemetry.substring(44, 47);
                    TTTT = telemetry.substring(60, 66);
                    PPPP = telemetry.substring(76, 83);
                    TelemetryOutput += LLL + "," + RRR + "," + AAA + "," + TTTT + "," + PPPP + "\r\n";
                }
            }
        }catch (Exception e){
            System.out.println(e);
        }
        try {
            copter_stream.write(TelemetryOutput.getBytes());
            copter_stream.close();
        } catch (IOException x) {
            System.out.println("(Copter) Failure saving the results.");
        }
    }

    private void storeData() {
        LOGGER.log(Level.INFO, "Writing packets to files.");
        var localFileWriter = new LocalCSVFileWriter();
        try {
            localFileWriter.writeStringsToFile("data/copter_" + requestCode + "_", receivedMessages,
                    new String[]{"left_motor", "right_motor", "altitude", "temperature", "pressure"});
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
        LOGGER.log(Level.INFO, "Finished writing packets to files.");
    }
}
