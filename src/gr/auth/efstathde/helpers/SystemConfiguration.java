package gr.auth.efstathde.helpers;

public class SystemConfiguration {
    private static final String SERVER_IP = "155.207.18.208";
    private static final int SERVER_PORT = 38015;
    private static final int CLIENT_PORT = 48015;

    public static String getServerIp() {
        return SERVER_IP;
    }

    public static int getServerPort() {
        return SERVER_PORT;
    }

    public static int getClientPort() {
        return CLIENT_PORT;
    }
}
