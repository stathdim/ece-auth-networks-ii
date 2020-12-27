package gr.auth.efstathde.helpers;

public class SystemConfiguration {
    private static final String SERVER_IP = "155.207.18.208";
    private static final int SERVER_PORT = 38011;
    private static final int CLIENT_PORT = 48011;
    private static final String PACKET_CODE = "E9167";
    private static final String IMAGE_CODE = "M3284";
    private static final String AUDIO_CODE = "A5086";
    private static final String COPTER_CODE = "Q5105";
    private static final String DIAGNOSTICS_CODE = "V8853";

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
