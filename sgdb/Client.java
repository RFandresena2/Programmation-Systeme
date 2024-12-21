package reseaux;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Demander le port et l'adresse du serveur
        System.out.print("Entrez l'adresse du serveur (par défaut : localhost) : ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) {
            host = "localhost"; // Valeur par défaut
        }

        System.out.print("Entrez le port du serveur (par défaut : 12345) : ");
        String portInput = scanner.nextLine().trim();
        int port;
        try {
            port = portInput.isEmpty() ? 12345 : Integer.parseInt(portInput); // Port par défaut : 12345
        } catch (NumberFormatException e) {
            System.out.println("Port invalide. Utilisation du port par défaut : 12345.");
            port = 12345;
        }

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("Connecté au serveur.");

            String message;
            while (true) {
                // Lire un message depuis le terminal
                System.out.print("Entrez une requête ('quit' pour terminer) : ");
                message = scanner.nextLine();

                // Envoyer le message au serveur
                out.println(message);

                // Si "quit", arrêter la boucle
                if (message.equalsIgnoreCase("quit")) {
                    System.out.println("Déconnexion...");
                    break;
                }

                // Lire la première ligne de la réponse
                String premiereLigne = in.readLine();

                if (premiereLigne != null && !premiereLigne.isEmpty()) {
                    if (in.ready()) { // Si d'autres lignes sont disponibles
                        System.out.println("Réponse du serveur :");
                        System.out.println(premiereLigne); // Affiche la première ligne
                        String ligneSuivante;
                        while ((ligneSuivante = in.readLine()) != null && !ligneSuivante.isEmpty()) {
                            System.out.println(ligneSuivante); // Affiche les lignes suivantes
                        }
                    } else { // Une seule ligne
                        System.out.println("Réponse du serveur : " + premiereLigne);
                    }
                } else {
                    System.out.println("Réponse du serveur vide.");
                }
            }

        } catch (IOException e) {
            System.out.println("Impossible de se connecter au serveur à " + host + " sur le port " + port);
            e.printStackTrace();
        }
    }
}
