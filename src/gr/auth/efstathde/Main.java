package gr.auth.efstathde;

import gr.auth.efstathde.services.*;

public class Main {

    public static void main(String[] args) {
        try
        {
//            var service = new PacketService();
//            service.getPacketsWithTemperature();
//            service.getPacketsWithoutTemperature();
//            var imageService = new ImageService();
//            imageService.getImage();
//            var copterService = new IthakiCopterService();
//            copterService.communicateWithCopter();
//            var soundService = new NonAdaptiveSoundService();
//            soundService.soundDPCM();
            var adaptiveSoundService = new AdaptiveSoundService();
            adaptiveSoundService.test();
//            var diagnosticsService = new DiagnosticsService();
//            diagnosticsService.GetDeviceDiagnostics();
        } catch (Exception ex)
        {
            System.out.println(ex.getMessage());
        }
    }
}
