package gr.auth.efstathde.helpers;

public class SystemConfiguration {
    private static final String SERVER_IP = "155.207.18.208";
    private static final int SERVER_PORT = 38024;
    private static final int CLIENT_PORT = 48024;
    private static final String PACKET_CODE = "E7094";
    private static final String IMAGE_CODE = "M8604";
    private static final String AUDIO_CODE = "A1197";
    private static final String COPTER_CODE = "Q2437";
    private static final String DIAGNOSTICS_CODE = "V4241";

    public static String getServerIp() {
        return SERVER_IP;
    }

    public static int getServerPort() {
        return SERVER_PORT;
    }

    public static int getClientPort() {
        return CLIENT_PORT;
    }

    public static String getAudioCode() {
        return AUDIO_CODE;
    }

    public static String getDiagnosticsCode() {
        return DIAGNOSTICS_CODE;
    }

    public static String getCopterCode() {
        return COPTER_CODE;
    }

    public static String getImageCode() {
        return IMAGE_CODE;
    }

    public static String getPacketCode() {
        return PACKET_CODE;
    }
}
