package gr.auth.efstathde;

import gr.auth.efstathde.services.PackageService;

public class Main {

    public static void main(String[] args) {
        try
        {
            var service = new PackageService();
            service.performPing();
        } catch (Exception ex)
        {
            System.out.println("Error");
        }
    }
}
