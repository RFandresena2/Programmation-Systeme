package reseaux;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import rlt.*;

public class ServeurBackup {

    private static final String CONFIG_FILE = "config.txt";

    // Méthode principale
    public static void main(String[] args) {
        try {
            // Charger les configurations
            Properties config = chargerConfiguration();

            // Lire le port depuis le fichier de configuration
            int port = Integer.parseInt(config.getProperty("portBackup", "")); // 12345 par défaut
            String logFile = config.getProperty("logFile", "server.log"); // Fichier de log par défaut
            int maxClients = Integer.parseInt(config.getProperty("maxClients", "100"));

            System.out.println("Serveur démarré avec les paramètres suivants :");
            System.out.println("Port : " + port);
            System.out.println("Max Clients : " + maxClients);
            System.out.println("Log File : " + logFile);

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Serveur en attente de connexions...");

                while (true) {
                    // Accepter une connexion client
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connecté : " + clientSocket.getInetAddress());

                    // Traiter la connexion client
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                         PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                        String messageClient;

                        while (true) {
                            // Lire le message du client
                            messageClient = in.readLine();

                            if (messageClient == null) {
                                System.out.println("Client déconnecté.");
                                break;
                            }

                            System.out.println("Requête reçue : " + messageClient);

                            // Si le client envoie "quit", arrêter la boucle
                            if (messageClient.equalsIgnoreCase("quit")) {
                                System.out.println("Client a envoyé 'quit'. Fermeture de la connexion.");
                                out.println("Connexion terminée.");
                                break;
                            }

                            // Traiter la requête et répondre au client
                            String reponse = traiterRequete(messageClient);
                            out.println(reponse);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Erreur : Le port ou maxClients dans le fichier de configuration est invalide.");
            e.printStackTrace();
        }
    }

    // Méthode pour charger les configurations depuis un fichier
    private static Properties chargerConfiguration() {
        Properties config = new Properties();
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            config.load(reader);
        } catch (FileNotFoundException e) {
            System.err.println("Fichier de configuration '" + CONFIG_FILE + "' non trouvé. Utilisation des valeurs par défaut.");
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du fichier de configuration.");
            e.printStackTrace();
        }
        return config;
    }

    // Exemple de traitement des requêtes (méthode simplifiée pour ce contexte)
    private static String traiterRequete(String requete) {
        try {
            if (requete.startsWith("CREATE DATABASE")) {
                return "Impossible d'effectuez cette commande";
            } else if (requete.startsWith("USE")) {
                String nomBase = requete.substring("USE".length()).trim();
                return FichierGestion.utiliserBase(nomBase);
            } else if (requete.startsWith("CREATE TABLE")) {
                return "Impossible d'effectuez cette commande";
            } else if (requete.startsWith("INSERT INTO")) {
                return "Impossible d'effectuez cette commande";
            } else if (requete.startsWith("SELECT")) {
                Relation resultat = FichierGestion.executerSelect(requete);
    
                if (resultat == null || resultat.getLignes().isEmpty()) {
                    return "Aucun résultat trouvé.";
                }
    
                resultat.afficherRelation();
                String retour = "Requête SELECT exécutée avec succès."
                        .concat("\n")
                        .concat(resultat.obtenirAffichageRelation());
                return retour;
            } else if (requete.equalsIgnoreCase("SHOW DATABASES")) {
                return FichierGestion.showDatabases();
            } else if (requete.startsWith("DESC")) {
                return FichierGestion.descTable(requete);
            } else if (requete.equalsIgnoreCase("SHOW TABLES")) {
                return FichierGestion.afficherTables();
            } else if (requete.startsWith("DELETE FROM")) {
                return "Impossible d'effectuez cette commande";
            }
            else if (requete.startsWith("UPDATE")) {
                return "Impossible d'effectuez cette commande";
            }else {
                return "Erreur : Requête non reconnue.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur : Une exception est survenue lors du traitement de la requête.";
        }
    }
    
    

    // Méthode pour créer une nouvelle base de données
    private static String creerBase(String nomBase) {
        try {
            // Si la base existe déjà, ne pas la recréer
            if (FichierGestion.baseExiste(nomBase)) {
                return "La base '" + nomBase + "' existe déjà.";
            }

            // Créer la base de données et la sauvegarder dans le fichier
            Base nouvelleBase = new Base(nomBase);
            FichierGestion.sauvegarderBase(nouvelleBase);

            return "Base '" + nomBase + "' créée avec succès.";
        } catch (IOException e) {
            e.printStackTrace();
            return "Erreur lors de la création de la base.";
        }
    }
}
