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

/***
 * Connects to the Vehicle Diagnostics Service using the UDP protocol
 */
public class DiagnosticsService {
    private static final Logger LOGGER = Logger.getLogger(DiagnosticsService.class.getSimpleName());
    private static final String CODE = "V2219";
    private static final int DURATION_MS = 240000; // 4 min in milliseconds
    private static final String[] typesOfMeasurements = new String[]{
            "Run Time",
            "Air Temperature",
            "Throttle Position",
            "Engine RPM",
            "Speed",
            "Coolant Temperature"
    };

    // Template for String-backed enums in Java taken from https://stackoverflow.com/a/3978690
    private enum PID_CODES {
        ENGINE_RUN_TIME("1F"),
        INTAKE_AIR_TEMPERATURE("0F"),
        THROTTLE_POSITION("11"),
        ENGINE_RPM("0C"),
        VEHICLE_SPEED("0D"),
        COOLANT_TEMPERATURE("05");

        private final String text;

        /**
         * @param text
         */
        PID_CODES(final String text) {
            this.text = text;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return text;
        }
    }

    private static List<String[]> receivedDiagnostics;


    public DiagnosticsService() {
        receivedDiagnostics = new ArrayList<>();
    }

    private static float diagnosticsResponseFormula(String rx, int typeOfOperation) {
        String[] p = rx.split(" ");
        float XX = Integer.parseInt(p[2], 16); // We use a radix of 16 to parse a hexadecimal number
        float YY = Integer.parseInt(p[3].substring(0, 2), 16);

        switch (typeOfOperation) {
            case 0:
                return 256 * XX + YY;
            case 1:
            case 5:
                return XX - 40;
            case 2:
                return XX * 100 / 255;
            case 3:
                return ((XX * 256) + YY) / 4;
            case 4:
                return XX;
        }
        return 1;
    }

    public void GetDeviceDiagnostics() throws IOException {
        var ipAddress = SystemConfiguration.getServerIp();
        var serverPort = SystemConfiguration.getServerPort();
        var clientPort = SystemConfiguration.getClientPort();

        String[] requestData = getRequestData();

        byte[] rxbuffer = new byte[2048];
        DatagramPacket resPacket = new DatagramPacket(rxbuffer, rxbuffer.length);
        DatagramSocket resSocket = new DatagramSocket(clientPort);
        DatagramSocket reqSocket = new DatagramSocket();

        long end, dt = 0;
        long timeStart = System.currentTimeMillis();
        while (dt < DURATION_MS) {
            var diagnostics = new String[typesOfMeasurements.length];
            for (int i = 0; i < PID_CODES.values().length; i++) {
                byte[] txBuffer = requestData[i].getBytes();
                DatagramPacket reqPacket =
                        new DatagramPacket(txBuffer, txBuffer.length, InetAddress.getByName(ipAddress), serverPort);
                reqSocket.send(reqPacket);
                resSocket.setSoTimeout(3200);
                resSocket.receive(resPacket);
                double result = diagnosticsResponseFormula(new String(rxbuffer), i);
                diagnostics[i] = String.valueOf(result);
            }
            end = System.currentTimeMillis();
            dt = end - timeStart;
            receivedDiagnostics.add(diagnostics);
        }

        resSocket.close();
        reqSocket.close();

        storeData();
    }

    private String[] getRequestData() {
        var codeWithModifier = CODE + "OBD=01 ";

        String[] requestData = {
                codeWithModifier + PID_CODES.ENGINE_RUN_TIME.toString(),
                codeWithModifier + PID_CODES.INTAKE_AIR_TEMPERATURE.toString(),
                codeWithModifier + PID_CODES.THROTTLE_POSITION.toString(),
                codeWithModifier + PID_CODES.ENGINE_RPM.toString(),
                codeWithModifier + PID_CODES.VEHICLE_SPEED.toString(),
                codeWithModifier + PID_CODES.COOLANT_TEMPERATURE.toString()
        };
        return requestData;
    }

    private void storeData() {
        LOGGER.log(Level.INFO, "Writing packets to files.");
        var localFileWriter = new LocalCSVFileWriter();
        try {
            localFileWriter.writeToFile("data/diagnostics_" + CODE + "_", receivedDiagnostics, typesOfMeasurements);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
        LOGGER.log(Level.INFO, "Finished writing packets to files.");
    }
}
