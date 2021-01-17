package gr.auth.efstathde;

import gr.auth.efstathde.services.*;

public class Main {

    public static void main(String[] args) {
        try
        {
            var soundService = new NonAdaptiveSoundService();
            soundService.getSignals();
            var adaptiveSoundService = new AdaptiveSoundService();
            adaptiveSoundService.getSoundFile();
            var service = new PacketService();
            service.getPacketsWithTemperature();
            service.getPacketsWithoutTemperature();
            service.getPacketsWithDisabledRandomness();
            var imageService = new ImageService();
            imageService.getImage();
            var copterService = new IthakiCopterService();
            copterService.communicateWithCopter();
            var diagnosticsService = new DiagnosticsService();
            diagnosticsService.GetDeviceDiagnostics();
        } catch (Exception ex)
        {
            System.out.println(ex.getMessage());
        }
    }
}
