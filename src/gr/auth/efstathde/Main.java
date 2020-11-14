package gr.auth.efstathde;

import gr.auth.efstathde.services.PacketService;

public class Main {

    public static void main(String[] args) {
        try
        {
            var service = new PacketService();
            service.getPacketsWithTemperature();
            service.getPacketsWithoutTemperature();
        } catch (Exception ex)
        {
            System.out.println("Error");
        }
    }
}
