package reseaux;

import rlt.*;
import domaine.*;
import condition.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FichierGestion {

    private static final String FICHIER = "Data.txt";

    // Sauvegarde la base dans le fichier
    public static void sauvegarderBase(Base base) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FICHIER, true))) {
            writer.write("Base: " + base.getNom() + "\n");
            writer.write("Relations:\n");
            for (Relation relation : base.getRelations()) {
                writer.write("- " + relation.getNom() + "\n");
                writer.write("  Attributs: " + Arrays.toString(relation.getAttributs()) + "\n");
                writer.write("  Lignes:\n");
                for (Ligne ligne : relation.getLignes()) {
                    writer.write("    " + Arrays.toString(ligne.getElements()) + "\n");
                }
            }
            writer.write("\n");
        }
    }

    // Vérifie si une base existe déjà dans le fichier
    public static boolean baseExiste(String nomBase) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(FICHIER))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                if (ligne.startsWith("Base: " + nomBase)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String utiliserBase(String nomBase) {
        try (BufferedReader reader = new BufferedReader(new FileReader("Data.txt"))) {
            boolean baseExiste = false;
            StringBuilder contenuFichier = new StringBuilder();
            StringBuilder autresLignes = new StringBuilder();
            String ligne;

            // Lire toutes les lignes et vérifier si la base existe
            while ((ligne = reader.readLine()) != null) {
                if (ligne.equals("Base: " + nomBase)) {
                    baseExiste = true;
                }
                if (!ligne.startsWith("ActiveBase:")) {
                    autresLignes.append(ligne).append(System.lineSeparator());
                }
            }

            if (!baseExiste) {
                return "Erreur : La base '" + nomBase + "' n'existe pas.";
            }

            // Ajouter ou mettre à jour la ligne ActiveBase
            contenuFichier.append("ActiveBase: ").append(nomBase).append(System.lineSeparator());
            contenuFichier.append(autresLignes);

            // Écrire les modifications dans le fichier
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("Data.txt"))) {
                writer.write(contenuFichier.toString());
            } catch (Exception e) {
                return "Erreur lors de l'écriture dans le fichier.";
            }
            return "Base '" + nomBase + "' sélectionnée comme base active.";
        } catch (IOException e) {
            e.printStackTrace();
            return "Erreur lors de l'accès au fichier des bases.";
        }
    }

    private static String obtenirBaseActive() {
        try (BufferedReader reader = new BufferedReader(new FileReader("Data.txt"))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                if (ligne.startsWith("ActiveBase: ")) {
                    return ligne.substring("ActiveBase: ".length()).trim();
                }
            }
            return null; // Pas de base active définie
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String creerTable(String requete) {
        try {
            // Lire la base active
            String baseActive = obtenirBaseActive();
            if (baseActive == null) {
                return "Erreur : Aucune base active n'est définie. Utilisez 'USE <nom_base>' pour en sélectionner une.";
            }

            // Extraire le nom de la table et ses attributs depuis la requête
            Pattern pattern = Pattern.compile("CREATE TABLE (\\w+) \\((.+)\\)");
            Matcher matcher = pattern.matcher(requete);
            if (!matcher.find()) {
                return "Erreur : Syntaxe invalide. La requête doit être de la forme 'CREATE TABLE nom_table (attribut1 {domaine}, ...)'";
            }

            String nomTable = matcher.group(1);
            String attributsDef = matcher.group(2);

            // Vérifier si la table existe déjà
            try (BufferedReader reader = new BufferedReader(new FileReader("Data.txt"))) {
                String ligne;
                boolean baseTrouvee = false;

                while ((ligne = reader.readLine()) != null) {
                    if (ligne.equals("Base: " + baseActive)) {
                        baseTrouvee = true;
                    } else if (baseTrouvee && ligne.equals("  - Table: " + nomTable)) {
                        return "Erreur : La table '" + nomTable + "' existe déjà dans la base '" + baseActive + "'.";
                    } else if (baseTrouvee && ligne.startsWith("Base: ")) {
                        // Une autre base commence, on peut arrêter la recherche
                        break;
                    }
                }
            }

            // Construire la définition de la table
            StringBuilder tableDefinition = new StringBuilder();
            tableDefinition.append("  - Table: ").append(nomTable).append(System.lineSeparator());
            tableDefinition.append("    Attributs:").append(System.lineSeparator());

            // Parsing des attributs en respectant le domaine complet sur une ligne
            String[] attributs = attributsDef.split(",");
            for (int i = 0; i < attributs.length; i++) {
                String attribut = attributs[i].trim();
                if (attribut.contains("{")) {
                    // Rassembler les parties d'un domaine entre accolades
                    StringBuilder domaineComplet = new StringBuilder(attribut);
                    while (!attribut.endsWith("}")) {
                        i++; // Passer à la partie suivante
                        attribut = attributs[i].trim();
                        domaineComplet.append(", ").append(attribut);
                    }
                    tableDefinition.append("      - ").append(domaineComplet).append(System.lineSeparator());
                } else {
                    tableDefinition.append("      - ").append(attribut).append(System.lineSeparator());
                }
            }
            tableDefinition.append("    Lignes:").append(System.lineSeparator());

            // Ajouter la table au fichier
            try (BufferedReader reader = new BufferedReader(new FileReader("Data.txt"));
                    BufferedWriter writer = new BufferedWriter(new FileWriter("Data_temp.txt"))) {

                String ligne;
                boolean baseTrouvee = false;
                while ((ligne = reader.readLine()) != null) {
                    writer.write(ligne);
                    writer.newLine();

                    if (ligne.equals("Base: " + baseActive)) {
                        baseTrouvee = true;
                    } else if (baseTrouvee && ligne.startsWith("Base: ")) {
                        // Ajouter la définition de la table avant une nouvelle base ou fin du fichier
                        writer.write(tableDefinition.toString());
                        writer.newLine();
                        baseTrouvee = false;
                    }
                }

                // Si la base active est trouvée mais pas suivie d'une autre base, ajouter la
                // table
                if (baseTrouvee) {
                    writer.write(tableDefinition.toString());
                    writer.newLine();
                }
            }

            // Renommer le fichier temporaire pour remplacer l'ancien
            File oldFile = new File("Data.txt");
            File newFile = new File("Data_temp.txt");
            if (!oldFile.delete() || !newFile.renameTo(oldFile)) {
                return "Erreur : Impossible de mettre à jour le fichier des bases.";
            }

            return "Table '" + nomTable + "' créée avec succès dans la base '" + baseActive + "'.";
        } catch (IOException e) {
            e.printStackTrace();
            return "Erreur : Une erreur est survenue lors de la création de la table.";
        }
    }

    public static String insererDonnees(String requete) throws Exception {
        try {
            // Extraire le nom de la table et les valeurs à insérer
            String pattern = "INSERT INTO\\s+(\\w+)\\s*\\((.*?)\\)\\s*VALUES\\s*\\((.*?)\\)";
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(requete);

            if (!matcher.find()) {
                return "Erreur : Syntaxe incorrecte pour la requête INSERT INTO.";
            }

            String nomTable = matcher.group(1);
            String colonnesStr = matcher.group(2).trim();
            String valeursStr = matcher.group(3).trim();

            // Vérifier si une base est active
            String baseActive = obtenirBaseActive();
            if (baseActive == null) {
                return "Erreur : Aucune base active. Utilisez 'USE <nom_base>' pour sélectionner une base.";
            }

            // Vérifier si la table existe
            if (!verifierExistenceRelation(baseActive, nomTable)) {
                return "La table " + nomTable + " n'existe pas";
            }

            // Charger la table
            Relation table = chargerRelation(baseActive, nomTable);

            // Parser les colonnes et les valeurs
            String[] colonnes = colonnesStr.split(",");
            String[] valeurs = valeursStr.split(",");
            for (int i = 0; i < colonnes.length; i++) {
                colonnes[i] = colonnes[i].trim();
            }
            for (int i = 0; i < valeurs.length; i++) {
                valeurs[i] = valeurs[i].trim().replace("\"", "");
                System.out.println("valeur prime = " + valeurs[i]);
            }

            // Initialiser un tableau pour stocker les valeurs des attributs (avec des
            // valeurs par défaut null)
            Object[] valeursObjets = new Object[table.getAttributs().length];
            Arrays.fill(valeursObjets, null); // Initialiser toutes les valeurs à null
            // Mapper les colonnes spécifiées aux valeurs correspondantes
            int[] index = getIndiceColonne(colonnes, table);
            for (int i = 0; i < colonnes.length; i++) {
                Attribut attribut = table.getAttributParNom(colonnes[i]);
                if (attribut == null) {
                    return "Erreur : L'attribut '" + colonnes[i] + "' n'existe pas dans la table.";
                }
                // Obtenir l'index de l'attribut et assigner la valeur
                System.out.println("valeur i =" + valeurs[i]);
                try {
                    valeursObjets[index[i]] = convertirValeur(attribut.getDomaine(), valeurs[i]);
                } catch (Exception e) {
                    return e.getMessage();
                }
            }

            for (int j = 0; j < valeursObjets.length; j++) {

                System.out.println("valeur =" + valeursObjets[j]);
            }
            // Construire une ligne et valider
            Ligne nouvelleLigne = new Ligne(valeursObjets);
            try {
                table.verifierLigne(nouvelleLigne);
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }

            // Ajouter la ligne à la table
            table.ajouterLigne(nouvelleLigne);

            // Mettre à jour le fichier
            mettreAJourFichierInsertion(baseActive, table);

            return "Données insérées avec succès dans la table '" + nomTable + "'.";
        } catch (IllegalArgumentException e) {
            return "Erreur : " + e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
            return "Erreur : Une exception est survenue lors de l'insertion des données.";
        }
    }

    private static boolean verifierExistenceRelation(String nomBase, String nomRelation) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader("Data.txt"))) {
            String ligne;
            boolean baseTrouvee = false;

            while ((ligne = reader.readLine()) != null) {
                // Vérifier si c'est la base recherchée
                if (ligne.startsWith("Base: " + nomBase)) {
                    baseTrouvee = true;
                } else if (ligne.startsWith("Base: ") && baseTrouvee) {
                    // Nouvelle base rencontrée, arrêter la recherche
                    break;
                }

                // Si dans la bonne base, chercher la table
                if (baseTrouvee && ligne.startsWith("  - Table: ")) {
                    String nomRelationExtraite = ligne.substring("  - Table: ".length()).trim();
                    if (nomRelationExtraite.equals(nomRelation)) {
                        // Relation trouvée
                        return true;
                    }
                }
            }
        }

        // Base ou table non trouvée
        return false;
    }

    private static Relation chargerRelation(String nomBase, String nomTable)
            throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader("Data.txt"))) {
            String ligne;
            boolean baseTrouvee = false;
            boolean tableTrouvee = false;

            String nomRelation = null;
            List<Attribut> attributs = new ArrayList<>();
            List<Ligne> lignes = new ArrayList<>();

            while ((ligne = reader.readLine()) != null) {
                ligne = ligne.trim();

                // Identifier la base
                if (ligne.startsWith("Base: ")) {
                    if (ligne.equals("Base: " + nomBase)) {
                        baseTrouvee = true;
                    } else if (baseTrouvee) {
                        // Une autre base commence, fin de la recherche
                        break;
                    }
                }

                // Identifier la table
                if (baseTrouvee && ligne.startsWith("- Table: ")) {
                    String nomTableCourante = ligne.substring("- Table: ".length()).trim();
                    if (nomTableCourante.equals(nomTable)) {
                        tableTrouvee = true;
                        nomRelation = nomTable;
                    } else if (tableTrouvee) {
                        // Une autre table commence, fin de la recherche
                        break;
                    }
                }

                // Lire les attributs
                if (tableTrouvee && ligne.equals("Attributs:")) {
                    while ((ligne = reader.readLine()) != null && estPremierCaractere(ligne, '-')) {
                        ligne = ligne.trim().substring(2); // Supprimer le "- "

                        // Extraire nom et domaine de l'attribut
                        String[] parts = ligne.split(" ", 2);
                        if (parts.length != 2) {
                            throw new IOException("Format invalide pour les attributs : " + ligne);
                        }

                        String nomAttribut = parts[0].trim();
                        String domaineDef = parts[1].trim();
                        String domaineString = determinerDomaine(domaineDef);
                        domaineDef = enleverAccolades(domaineDef);
                        Domaine domaine = null;
                        // Déterminer le domaine
                        System.out.println("DomaineDef = " + domaineDef);
                        if (domaineString.equals("DomaineFini")) {
                            // Domaine fini
                            String[] valeurs = domaineDef.split(",");
                            Set<Object> elements = new HashSet<>();
                            for (String valeur : valeurs) {
                                elements.add(convertirValeurSimple(valeur.trim()));
                            }
                            domaine = new DomaineFini(elements);
                        } else if (domaineString.equals("DomaineInfini")) {
                            // Domaine infini (exemple : String, Integer)
                            List<Class<?>> types = new ArrayList<>();
                            String[] classes = domaineDef.split(",");
                            for (String classe : classes) {
                                System.out.println("classe " + classe);
                                System.out.println("Ato");
                                types.add(convertirEnClasse(classe.trim()));
                            }
                            domaine = new DomaineInfini(types);
                        }

                        else if (domaineString.equals("DomaineMixte")) {
                            // Identifier les parties : types (avant les virgules non numériques) et valeurs
                            // (numériques ou entre guillemets)
                            // String domaineDefSansAccolades = domaineDef.substring(1, domaineDef.length()
                            // - 1).trim();
                            String[] composants = domaineDef.split(",");

                            List<Class<?>> types = new ArrayList<>();
                            Set<Object> elements = new HashSet<>();

                            for (String composant : composants) {
                                composant = composant.trim();
                                if (composant.matches("^\".*\"$") || composant.matches("^-?\\d+(\\.\\d+)?$")) {
                                    // C'est une valeur numérique ou une chaîne entre guillemets
                                    elements.add(convertirValeurSimple(composant));
                                } else {
                                    // C'est un type (ex : String, Integer, etc.)
                                    types.add(convertirEnClasse(composant));
                                }
                            }

                            // Créer le DomaineMixte
                            domaine = new DomaineMixte(types, elements);
                        }

                        // Ajouter l'attribut
                        attributs.add(new Attribut(domaine, nomAttribut));

                    }
                }
                // System.out.println("++++++++++++++++++++++++++++");
                // System.out.println("ligne fichier = " + ligne);
                if (tableTrouvee) {
                    System.out.println(" table trouver");
                }
                if (ligne.trim().equals("Lignes:")) {
                    // System.out.println("equals");
                }
                // Lire les lignes
                if (tableTrouvee && ligne.trim().equals("Lignes:")) {
                    // System.out.println("ligne = " + ligne);
                    while ((ligne = reader.readLine()) != null && estPremierCaractere(ligne, '-')) {
                        String[] valeurs = ligne.substring(2).trim().split(",");
                        valeurs[0] = valeurs[0].substring(2);
                        Object[] ligneElements = new Object[valeurs.length];
                        // Convertir chaque élément
                        for (int i = 0; i < valeurs.length; i++) {
                            try {
                                ligneElements[i] = convertirValeur(attributs.get(i).getDomaine(), valeurs[i].trim());
                            } catch (Exception e) {
                                throw e;
                            }
                            // System.out.println("valeur ligne " + i + " =" + ligneElements[i]);
                        }

                        lignes.add(new Ligne(ligneElements));

                    }
                }
            }

            // Créer et retourner la relation
            if (nomRelation != null && !attributs.isEmpty()) {
                Relation relation = new Relation(nomRelation, attributs.toArray(new Attribut[0]));
                for (Ligne ligneObj : lignes) {
                    relation.ajouterLigne(ligneObj);
                }
                // System.out.println("affichage");
                relation.afficherRelation();
                return relation;
            }

            throw new IOException(
                    "La table '" + nomTable + "' dans la base '" + nomBase + "' n'a pas pu être trouvée.");
        }
    }

    private static Object convertirValeur(Domaine domaine, String valeur) throws Exception {
        // System.out.println(domaine);
        if (domaine instanceof DomaineFini) {
            // Si le domaine est un DomaineFini, vous pouvez chercher la valeur dans le
            // domaine fini
            Set<Object> elements = ((DomaineFini) domaine).getElements();
            valeur = valeur.trim();
            // System.out.println("valeur compare =" + valeur);
            for (Object element : elements) {
                System.out.println("element = " + element.toString());
                if (element.toString().equals(valeur) || valeur.equals("null")) {
                    return convertirValeurSimple(valeur);
                }
            }
            throw new Exception("la valeur " + valeur + " ne correspond pas au domaine " + domaine);
        } else if (domaine instanceof DomaineInfini) {
            System.out.println("infini");
            // Si le domaine est un DomaineInfini, la valeur doit correspondre à un type
            Class<?> type = null;
            for (int i = 0; i < ((DomaineInfini) domaine).getTypes().size(); i++) {
                try {
                    type = ((DomaineInfini) domaine).getTypes().get(i); // Par exemple, ici on prend juste le
                                                                        // premier type

                    return type.getConstructor(String.class).newInstance(valeur);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (domaine instanceof DomaineMixte) {
            Object[] element = ((DomaineMixte) domaine).getElements().toArray();
            for (int i = 0; i < element.length; i++) {
                if (valeur.equals(element[i].toString())) {
                    return convertirValeurSimple(valeur);
                }
            }
            Class<?> type = null;
            for (int i = 0; i < ((DomaineMixte) domaine).getTypes().size(); i++) {
                try {
                    type = ((DomaineMixte) domaine).getTypes().get(i); // Par exemple, ici on prend juste le
                                                                       // premier type
                    return type.getConstructor(String.class).newInstance(valeur);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null; // Si aucun domaine ne correspond
    }

    public static Class<?> convertirEnClasse(String nomClasse) throws ClassNotFoundException {
        switch (nomClasse) {
            // Types primitifs
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "boolean":
                return boolean.class;

            // Types primitifs en boîte
            case "Byte":
                return Byte.class;
            case "Short":
                return Short.class;
            case "Integer":
                return Integer.class;
            case "Long":
                return Long.class;
            case "Float":
                return Float.class;
            case "Double":
                return Double.class;
            case "Character":
                return Character.class;
            case "Boolean":
                return Boolean.class;

            // Chaîne de caractères
            case "String":
                return String.class;

            default:
                // Si ce n'est pas un type primitif ou standard, on tente de charger la classe
                // dynamiquement
                return Class.forName(nomClasse);
        }
    }

    public static boolean estPremierCaractere(String ligne, char caractere) {
        if (ligne == null || ligne.isEmpty()) {
            return false; // Ligne vide ou null, renvoie false
        }

        // Parcourir la ligne jusqu'au premier caractère non-espace
        for (int i = 0; i < ligne.length(); i++) {
            if (!Character.isWhitespace(ligne.charAt(i))) {
                return ligne.charAt(i) == caractere;
            }
        }

        return false; // Aucun caractère non-espace trouvé
    }

    public static String determinerDomaine(String input) {
        if (input == null || !input.startsWith("{") || !input.endsWith("}")) {
            throw new IllegalArgumentException("Le format du domaine est invalide : " + input);
        }

        // Supprimer les accolades
        String contenu = input.substring(1, input.length() - 1).trim();
        String[] elements = contenu.split(",");

        boolean contientType = false;
        boolean contientValeur = false;

        for (String element : elements) {
            element = element.trim();
            if (element.isEmpty()) {
                continue;
            }

            // Vérifier si l'élément est un type valide
            try {
                Class.forName("java.lang." + element);
                contientType = true;
            } catch (ClassNotFoundException e) {
                // Vérifier si c'est une valeur valide
                if (estValeur(element)) {
                    contientValeur = true;
                } else {
                    throw new IllegalArgumentException("Élément non valide dans le domaine : " + element);
                }
            }
        }

        if (contientType && contientValeur) {
            return "DomaineMixte";
        } else if (contientType) {
            return "DomaineInfini";
        } else if (contientValeur) {
            return "DomaineFini";
        } else {
            throw new IllegalArgumentException("Impossible de déterminer le type de domaine.");
        }
    }

    /**
     * Vérifie si une chaîne représente une valeur valide (entier, double, chaîne
     * entre guillemets).
     *
     * @param valeur La chaîne à vérifier.
     * @return true si c'est une valeur valide, false sinon.
     */
    private static boolean estValeur(String valeur) {
        try {
            Integer.parseInt(valeur);
            return true;
        } catch (NumberFormatException ignored) {
        }

        try {
            Double.parseDouble(valeur);
            return true;
        } catch (NumberFormatException ignored) {
        }

        // Vérifier si c'est une chaîne entre guillemets
        return valeur.startsWith("\"") && valeur.endsWith("\"");
    }

    public static String enleverAccolades(String input) {
        if (input.startsWith("{") && input.endsWith("}")) {
            return input.substring(1, input.length() - 1);
        }
        return input; // Retourne la chaîne d'origine si elle n'a pas les accolades
    }

    public static void traiterUpdate(String requete) throws Exception {
        // Étape 1 : Analyse de la requête UPDATE
        String pattern = "UPDATE\\s+(\\w+)\\s+SET\\s+([^\\s]+\\s*=\\s*[^\\s]+(?:,\\s*[^\\s]+\\s*=\\s*[^\\s]+)*)\\s*(WHERE\\s+(.*))?";
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = regex.matcher(requete);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Erreur : Syntaxe incorrecte pour la requête UPDATE.");
        }

        String nomTable = matcher.group(1).trim(); // Nom de la table
        String modificationsStr = matcher.group(2).trim(); // Partie SET
        String conditionsStr = matcher.group(4); // Partie WHERE (peut être null)

        // Étape 2 : Charger la relation
        Relation table = chargerRelation(obtenirBaseActive(), nomTable);
        if (table == null) {
            throw new IllegalArgumentException("Erreur : La table '" + nomTable + "' n'existe pas.");
        }
        System.out.println("Table chargée : " + nomTable);

        // Étape 3 : Parser les modifications (SET)
        Map<String, Object> modifications = new HashMap<>();
        String[] modificationsArray = modificationsStr.split(",");
        for (String modification : modificationsArray) {
            String[] parts = modification.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Erreur : Syntaxe incorrecte pour la clause SET : " + modification);
            }
            String attribut = parts[0].trim();
            String valeurStr = parts[1].trim().replace("\"", ""); // Supprimer les guillemets éventuels
            Attribut attributObj = table.getAttributParNom(attribut);

            if (attributObj == null) {
                throw new IllegalArgumentException(
                        "Erreur : L'attribut '" + attribut + "' n'existe pas dans la table.");
            }

            // Convertir la valeur selon le domaine de l'attribut
            Object valeur = convertirValeur(attributObj.getDomaine(), valeurStr);
            modifications.put(attribut, valeur);
        }
        System.out.println("Modifications : " + modifications);

        // Étape 4 : Parser les conditions WHERE, si présentes
        Condition[][] conditions = null;
        if (conditionsStr != null) {
            conditions = parserConditions(conditionsStr, table);
            System.out.println("Conditions WHERE analysées.");
        } else {
            System.out.println("Pas de conditions WHERE.");
        }

        // Étape 5 : Préparer le traitement futur
        // La relation `table` est prête, ainsi que les modifications et les conditions
        System.out.println("Prêt pour les modifications.");
    }

    private static String serializeTable(Relation table) {
        StringBuilder builder = new StringBuilder();
        builder.append("  - Table: ").append(table.getNom()).append(System.lineSeparator());
        builder.append("    Attributs:").append(System.lineSeparator());

        for (Attribut attribut : table.getAttributs()) {
            builder.append("      - ").append(attribut.getNom()).append(" {")
                    .append(serializeDomaine(attribut.getDomaine())).append("}").append(System.lineSeparator());
        }

        builder.append("    Lignes:").append(System.lineSeparator());
        for (Ligne ligne : table.getLignes()) {
            builder.append("      - ").append(Arrays.toString(ligne.getElements())
                    .replaceAll("[\\[\\]]", "")).append(System.lineSeparator());
        }

        return builder.toString();
    }

    private static String serializeDomaine(Domaine domaine) {
        if (domaine instanceof DomaineFini) {
            Set<Object> elements = ((DomaineFini) domaine).getElements();
            return elements.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
        } else if (domaine instanceof DomaineInfini) {
            List<Class<?>> types = ((DomaineInfini) domaine).getTypes();
            return types.stream()
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", "));
        } else if (domaine instanceof DomaineMixte) {
            DomaineMixte mixte = (DomaineMixte) domaine;
            String infiniPart = mixte.getTypes().stream()
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", "));
            String finiPart = mixte.getElements().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return infiniPart + "," + finiPart;
        }
        return "";
    }

    private static Object convertirValeurSimple(String valeur) {
        if (valeur == "null") {
            return null;
        }
        try {
            return Integer.parseInt(valeur);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(valeur);
            } catch (NumberFormatException e2) {
                return valeur; // Retourne la valeur en tant que chaîne si ce n'est pas un nombre
            }
        }
    }

    private static void mettreAJourFichierInsertion(String base, Relation relation) throws IOException {
        File inputFile = new File("Data.txt");
        File tempFile = new File("Data_temp.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String ligne;
            boolean baseTrouvee = false;
            boolean tableTrouvee = false;

            while ((ligne = reader.readLine()) != null) {
                // Vérifier si c'est la base recherchée
                if (ligne.equals("Base: " + base)) {
                    baseTrouvee = true;
                    writer.write(ligne);
                    writer.newLine();
                    continue;
                }

                // Si une autre base commence, écrire normalement
                if (ligne.startsWith("Base: ") && baseTrouvee && tableTrouvee) {
                    baseTrouvee = false; // Fin de la base courante
                }

                // Si dans la base active, chercher la table
                if (baseTrouvee && ligne.startsWith("  - Table: " + relation.getNom())) {
                    tableTrouvee = true;

                    // Sauter les lignes de l'ancienne table
                    while ((ligne = reader.readLine()) != null && !ligne.startsWith("  - Table: ")
                            && !ligne.startsWith("Base: ")) {
                        // Skip old table data
                    }

                    // Écrire la nouvelle table
                    writer.write(serializeTable(relation));
                    writer.newLine();

                    // Si fin de fichier ou début d'une autre base/table, ne pas sauter
                    if (ligne != null && !ligne.startsWith("Base: ")) {
                        writer.write(ligne);
                        writer.newLine();
                    }

                    continue;
                }

                // Copier les autres lignes normalement
                writer.write(ligne);
                writer.newLine();
            }

            // Si la table n'a pas été trouvée dans la base active, on peut ajouter à la fin
            if (baseTrouvee && !tableTrouvee) {
                writer.write(serializeTable(relation));
            }
        }

        // Remplacer l'ancien fichier par le nouveau
        if (!inputFile.delete() || !tempFile.renameTo(inputFile)) {
            throw new IOException("Erreur lors de la mise à jour du fichier Data.txt.");
        }
    }

    public static int[] getIndiceColonne(String[] colonne, Relation relation) {
        int[] retour = new int[colonne.length];
        for (int i = 0; i < retour.length; i++) {
            for (int j = 0; j < relation.getAttributs().length; j++) {
                if (colonne[i].equals(relation.getAttributs()[j].getNom())) {
                    retour[i] = j;
                }
            }
        }
        return retour;
    }

    public static Relation executerSelect(String requete) throws Exception {
        Relation retour = null;
        if (requete.contains("JOIN")) {
            if (requete.contains("LEFT")) {
                retour = executerSelectAvecLeftJoin(requete);
            } else if (requete.contains("RIGHT")) {
                retour = executerSelectAvecRightJoin(requete);
            } else if (requete.contains("OUTER")) {
                retour = executerSelectAvecOuterJoin(requete);
            } else {
                retour = executerRequeteAvecJoin(requete);
            }
        } else {
            retour = executerSelectNormal(requete);
        }
        return retour;
    }

    public static Relation executerSelectNormal(String requete) throws Exception {
        try {
            // Étape 1 : Parse de la requête
            String pattern = "SELECT\\s+(.*?)\\s+FROM\\s+(\\w+)(?:\\s+WHERE\\s+(.*))?";
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(requete);

            if (!matcher.find()) {
                throw new IllegalArgumentException("Erreur : Syntaxe incorrecte pour la requête SELECT.");
            }

            String colonnesStr = matcher.group(1).trim(); // Colonnes à sélectionner
            String nomTable = matcher.group(2).trim(); // Nom de la table
            String conditionsStr = matcher.group(3); // Clause WHERE (peut être null)

            // Étape 2 : Charger la relation
            Relation table = chargerRelation(obtenirBaseActive(), nomTable);
            if (table == null) {
                throw new IllegalArgumentException("Erreur : La table '" + nomTable + "' n'existe pas.");
            }

            // Étape 3 : Appliquer la sélection (WHERE)
            Relation relationFiltree = table; // Par défaut, la relation entière
            if (conditionsStr != null) {
                // Analyse de la clause WHERE
                Condition[][] conditions = parserConditions(conditionsStr, table);
                relationFiltree = relationFiltree.selection(conditions);
            }

            // Étape 4 : Appliquer la projection (SELECT)
            if (!colonnesStr.equals("*")) {
                // Extraire les noms des colonnes à projeter
                String[] colonnes = colonnesStr.split(",");
                ArrayList<String> nomsColonnes = new ArrayList<>();
                for (String colonne : colonnes) {
                    nomsColonnes.add(colonne.trim());
                }

                // Appliquer la projection
                relationFiltree = relationFiltree.projection(nomsColonnes);
            }

            // Retourner la relation projetée
            return relationFiltree;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du traitement de la requête SELECT.");
        }
    }

    private static String[] diviserParOr(String clauseWhere) {
        List<String> blocsOr = new ArrayList<>();
        StringBuilder blocActuel = new StringBuilder();
        int parentheseCount = 0;

        for (int i = 0; i < clauseWhere.length(); i++) {
            char c = clauseWhere.charAt(i);

            // Ajouter le caractère actuel au bloc
            blocActuel.append(c);

            // Mise à jour du compteur de parenthèses
            if (c == '(') {
                parentheseCount++;
            } else if (c == ')') {
                parentheseCount--;
            }

            // Si on rencontre un `OR` hors des parenthèses, découper
            if (parentheseCount == 0 && clauseWhere.substring(i).matches("(?i)^\\s*OR\\b.*")) {
                // Ajouter le bloc actuel à la liste et réinitialiser
                blocsOr.add(blocActuel.toString().trim().substring(0, blocActuel.length() - 1));
                blocActuel.setLength(0);

                // Sauter les caractères "OR" et continuer après l'espace
                i += 2; // Avancer de 2 pour "OR"
                while (i < clauseWhere.length() && Character.isWhitespace(clauseWhere.charAt(i))) {
                    i++; // Ignorer les espaces
                }
                i--; // Réajuster car la boucle principale fait un `i++`
            }
        }

        // Ajouter le dernier bloc restant
        if (blocActuel.length() > 0) {
            blocsOr.add(blocActuel.toString().trim());
        }
        for (int i = 0; i < blocsOr.size(); i++) {
            System.out.println("bloc fo " + blocsOr.get(i));
        }
        return blocsOr.toArray(new String[0]);
    }

    public static Condition[][] parserConditions(String clauseWhere, Relation relation) throws Exception {
        // Supprimer les parenthèses inutiles et espaces inutiles
        clauseWhere = clauseWhere.trim().replaceAll("\\s+\\(", "(").replaceAll("\\(\\s+", "(")
                .replaceAll("\\s+\\)", ")").replaceAll("\\)\\s+", ")");

        // Diviser les conditions principales selon les OR
        String[] blocsOr = diviserParOr(clauseWhere); // Division par OR, insensible à la casse
        System.out.println("taille " + blocsOr.length);
        List<Condition[]> conditionsGlobales = new ArrayList<>();

        // Parcourir chaque bloc séparé par OR
        for (String bloc : blocsOr) {
            // Diviser chaque bloc selon les AND
            System.out.println("bloc = " + bloc);
            String[] blocsAnd = bloc.split("(?i)\\s+AND\\s+"); // Division par AND
            List<Condition> conditionsLocales = new ArrayList<>();

            for (String condition : blocsAnd) {
                // Extraire l'attribut, l'opérateur et la valeur depuis la condition
                String pattern = "(\\w+)\\s*(=|!=|>|>=|<|<=)\\s*(\\w+|\\d+|\"[^\"]+\")";
                Pattern regex = Pattern.compile(pattern);
                Matcher matcher = regex.matcher(condition);

                if (matcher.find()) {
                    String attributNom = matcher.group(1).trim(); // Ex : age
                    String operateur = matcher.group(2).trim(); // Ex : >
                    String valeurStr = matcher.group(3).trim(); // Ex : 10 ou "Alice"

                    // Trouver l'attribut dans la relation
                    Attribut attribut = relation.getAttributParNom(attributNom);
                    if (attribut == null) {
                        throw new IllegalArgumentException(
                                "L'attribut '" + attributNom + "' n'existe pas dans la table.");
                    }

                    // Convertir la valeur en objet selon le domaine de l'attribut
                    Object valeur = convertirValeur(attribut.getDomaine(), valeurStr);

                    // Créer la condition et l'ajouter à la liste locale
                    conditionsLocales.add(new Condition(valeur, operateur, attribut, relation));
                } else {
                    throw new IllegalArgumentException("Syntaxe de condition incorrecte : " + condition);
                }
            }

            // Ajouter les conditions locales (AND) comme un tableau
            conditionsGlobales.add(conditionsLocales.toArray(new Condition[0]));
        }
        for (int i = 0; i < conditionsGlobales.size(); i++) {
            for (int j = 0; j < conditionsGlobales.get(i).length; j++) {
                System.out.println("condition = " + conditionsGlobales.get(i)[j]);

            }
            System.out.println((i + "\n"));
        }
        // Convertir la liste globale (OR) en tableau bidimensionnel
        return conditionsGlobales.toArray(new Condition[0][0]);
    }

    public static String afficherTables() {
        try (BufferedReader reader = new BufferedReader(new FileReader("Data.txt"))) {
            String baseActive = obtenirBaseActive(); // Méthode pour obtenir la base active
            if (baseActive == null) {
                return "Erreur : Aucune base active. Utilisez 'USE <nom_base>' pour sélectionner une base.";
            }

            String ligne;
            boolean baseTrouvee = false;
            List<String> tables = new ArrayList<>();

            while ((ligne = reader.readLine()) != null) {
                // Rechercher la base active
                if (ligne.equals("Base: " + baseActive)) {
                    baseTrouvee = true;
                } else if (ligne.startsWith("Base: ") && baseTrouvee) {
                    // Une nouvelle base commence, fin de la base active
                    break;
                }

                // Si dans la base active, chercher les tables
                if (baseTrouvee && ligne.trim().startsWith("- Table: ")) {
                    String nomTable = ligne.substring("- Table: ".length()).trim();
                    tables.add(nomTable);
                }
            }

            if (tables.isEmpty()) {
                return "Aucune table trouvée dans la base '" + baseActive + "'.";
            }

            // Construire un affichage des tables
            StringBuilder affichage = new StringBuilder("Tables dans la base '").append(baseActive).append("':\n");
            for (String table : tables) {
                affichage.append("- ").append(table).append("\n");
            }

            return affichage.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "Erreur : Une exception est survenue lors de la lecture du fichier des bases.";
        }
    }

    public static Relation executerRequeteAvecJoin(String requete) throws Exception {
        try {
            // Étape 1 : Parse de la requête
            String pattern = "SELECT\\s+(.*?)\\s+FROM\\s+(\\w+)(?:\\s+(\\w+))?\\s+JOIN\\s+(\\w+)(?:\\s+(\\w+))?\\s+ON\\s+(.*?)(?:\\s+WHERE\\s+(.*))?$";
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(requete);

            if (!matcher.find()) {
                throw new IllegalArgumentException("Erreur : Syntaxe incorrecte pour la requête JOIN.");
            }

            String colonnesStr = matcher.group(1).trim(); // Colonnes à sélectionner
            String nomTable1 = matcher.group(2).trim(); // Première table
            String aliasTable1 = matcher.group(3); // Alias de la première table (peut être null)
            String nomTable2 = matcher.group(4).trim(); // Deuxième table
            String aliasTable2 = matcher.group(5); // Alias de la deuxième table (peut être null)
            String conditionJoin = matcher.group(6).trim(); // Condition ON
            String conditionsWhereStr = matcher.group(7); // Clause WHERE (peut être null)

            // Étape 2 : Charger les relations
            Relation relation1 = chargerRelation(obtenirBaseActive(), nomTable1);
            Relation relation2 = chargerRelation(obtenirBaseActive(), nomTable2);

            if (relation1 == null || relation2 == null) {
                throw new IllegalArgumentException("Erreur : Une ou plusieurs tables spécifiées n'existent pas.");
            }

            // Étape 3 : Créer les conditions de jointure
            Condition[][] conditionsJoin = parserConditionsJoin(conditionJoin, relation1, relation2);

            // Étape 4 : Appliquer la jointure
            for (int i = 0; i < conditionsJoin.length; i++) {
                for (int j = 0; j < conditionsJoin[i].length; j++) {
                    System.out.println("Condition" + i + " : " + conditionsJoin[i][j]);
                }
                System.out.println("\n");
            }
            Relation resultat = relation1.thetaJointure(relation2, conditionsJoin);

            // Étape 5 : Appliquer la clause WHERE, si elle existe
            if (conditionsWhereStr != null && !conditionsWhereStr.isEmpty()) {
                Condition[][] conditionsWhere = parserConditions(conditionsWhereStr, resultat);
                resultat = resultat.selection(conditionsWhere);
            }

            // Étape 6 : Appliquer la projection (SELECT)
            if (!colonnesStr.equals("*")) {
                String[] colonnes = colonnesStr.split(",");
                ArrayList<String> nomsColonnes = new ArrayList<>();
                for (String colonne : colonnes) {
                    nomsColonnes.add(colonne.trim());
                    System.out.println("colonnes" + colonne);
                }
                System.out.println("alias1" + aliasTable1);
                System.out.println("alias2" + aliasTable2);

                resultat = resultat.projection(nomsColonnes);
            }

            // Retourner la relation résultante
            return resultat;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du traitement de la requête JOIN.");
        }
    }

    private static Condition[][] parserConditionsJoin(String conditionJoin, Relation relation1, Relation relation2)
            throws Exception {
        // Diviser les conditions par AND (car les JOIN n'utilisent pas OR généralement)
        System.out.println("condition = " + conditionJoin);
        String[] conditionsAnd = conditionJoin.split("(?i)\\s+AND\\s+");
        List<Condition> conditionsLocales = new ArrayList<>();

        for (String condition : conditionsAnd) {
            // Extraire les deux attributs et l'opérateur
            String pattern = "(\\w+)\\.(\\w+)\\s*(=|!=|>|>=|<|<=)\\s*(\\w+)\\.(\\w+)";
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(condition);

            if (matcher.find()) {
                String table1 = matcher.group(1).trim(); // Première table
                String attribut1 = matcher.group(2).trim(); // Attribut de la première table
                String operateur = matcher.group(3).trim(); // Opérateur
                String table2 = matcher.group(4).trim(); // Deuxième table
                String attribut2 = matcher.group(5).trim(); // Attribut de la deuxième table

                // Récupérer les attributs des relations
                Attribut attr1 = relation1.getAttributParNom(attribut1);
                Attribut attr2 = relation2.getAttributParNom(attribut2);

                if (attr1 == null || attr2 == null) {
                    throw new IllegalArgumentException("Erreur : L'un des attributs spécifiés n'existe pas.");
                }

                // Ajouter la condition pour le JOIN
                conditionsLocales.add(new Condition(attr1, attr2, operateur, relation1, relation2));
            } else {
                throw new IllegalArgumentException("Syntaxe de condition JOIN incorrecte : " + condition);
            }
        }

        // Retourner un tableau de conditions (une seule dimension, car pas d'OR ici)
        return new Condition[][] { conditionsLocales.toArray(new Condition[0]) };
    }

    public static Relation executerSelectAvecLeftJoin(String requete) throws Exception {
        try {
            // Étape 1 : Parse de la requête avec les alias des tables
            String pattern = "SELECT\\s+(.*?)\\s+FROM\\s+(\\w+)\\s+(\\w+)\\s+LEFT\\s+JOIN\\s+(\\w+)\\s+(\\w+)\\s+ON\\s+((?:\\w+\\.\\w+\\s*=\\s*\\w+\\.\\w+)(?:\\s+AND\\s+\\w+\\.\\w+\\s*=\\s*\\w+\\.\\w+)*)\\s*(WHERE\\s+(.*))?";
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(requete);

            if (!matcher.find()) {
                throw new IllegalArgumentException(
                        "Erreur : Syntaxe incorrecte pour la requête SELECT avec LEFT JOIN.");
            }

            // Extraire les différentes parties de la requête
            String colonnesStr = matcher.group(1).trim(); // Colonnes à sélectionner
            String nomTable1 = matcher.group(2).trim(); // Première table
            String aliasTable1 = matcher.group(3).trim(); // Alias de la première table
            String nomTable2 = matcher.group(4).trim(); // Deuxième table (table à joindre)
            String aliasTable2 = matcher.group(5).trim(); // Alias de la deuxième table
            String conditionJoin = matcher.group(6).trim(); // Condition ON
            String conditionsWhereStr = matcher.group(8); // Clause WHERE (peut être null)

            // Debug : Affichage de la condition JOIN
            System.out.println("conditionJoin = " + conditionJoin);

            // Étape 2 : Charger les relations (tables)
            Relation table1 = chargerRelation(obtenirBaseActive(), nomTable1);
            Relation table2 = chargerRelation(obtenirBaseActive(), nomTable2);

            if (table1 == null || table2 == null) {
                throw new IllegalArgumentException("Erreur : Une ou plusieurs tables spécifiées n'existent pas.");
            }

            // Étape 3 : Créer les conditions de jointure (LEFT JOIN)
            // Vous pouvez ajuster `parserConditionsJoin` pour prendre en compte les alias
            Condition[][] conditionsJoin = parserConditionsJoin(conditionJoin, table1, table2);

            // Appliquer la jointure gauche (LEFT JOIN)
            Relation resultat = table1.leftJoin(table2, conditionsJoin);

            // Étape 4 : Appliquer la clause WHERE si elle existe
            if (conditionsWhereStr != null) {
                Condition[][] conditionsWhere = parserConditions(conditionsWhereStr, resultat);
                resultat = resultat.selection(conditionsWhere);
            }

            // Étape 5 : Appliquer la projection (SELECT)
            if (!colonnesStr.equals("*")) {
                String[] colonnes = colonnesStr.split(",");
                ArrayList<String> nomsColonnes = new ArrayList<>();
                for (String colonne : colonnes) {
                    nomsColonnes.add(colonne.trim());
                }
                if (!requete.contains("*")) {
                    resultat = resultat.projection(nomsColonnes);
                }
            }

            // Retourner la relation résultante
            return resultat;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du traitement de la requête SELECT avec LEFT JOIN.");
        }
    }

    public static Relation executerSelectAvecRightJoin(String requete) throws Exception {
        try {
            // Étape 1 : Parse de la requête avec les alias des tables
            String pattern = "SELECT\\s+(.*?)\\s+FROM\\s+(\\w+)\\s+(\\w+)\\s+RIGHT\\s+JOIN\\s+(\\w+)\\s+(\\w+)\\s+ON\\s+((?:\\w+\\.\\w+\\s*=\\s*\\w+\\.\\w+)(?:\\s+AND\\s+\\w+\\.\\w+\\s*=\\s*\\w+\\.\\w+)*)\\s*(WHERE\\s+(.*))?";
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(requete);

            if (!matcher.find()) {
                throw new IllegalArgumentException(
                        "Erreur : Syntaxe incorrecte pour la requête SELECT avec LEFT JOIN.");
            }

            // Extraire les différentes parties de la requête
            String colonnesStr = matcher.group(1).trim(); // Colonnes à sélectionner
            String nomTable1 = matcher.group(2).trim(); // Première table
            String aliasTable1 = matcher.group(3).trim(); // Alias de la première table
            String nomTable2 = matcher.group(4).trim(); // Deuxième table (table à joindre)
            String aliasTable2 = matcher.group(5).trim(); // Alias de la deuxième table
            String conditionJoin = matcher.group(6).trim(); // Condition ON
            String conditionsWhereStr = matcher.group(8); // Clause WHERE (peut être null)

            // Debug : Affichage de la condition JOIN
            System.out.println("conditionJoin = " + conditionJoin);

            // Étape 2 : Charger les relations (tables)
            Relation table1 = chargerRelation(obtenirBaseActive(), nomTable1);
            Relation table2 = chargerRelation(obtenirBaseActive(), nomTable2);

            if (table1 == null || table2 == null) {
                throw new IllegalArgumentException("Erreur : Une ou plusieurs tables spécifiées n'existent pas.");
            }

            // Étape 3 : Créer les conditions de jointure (LEFT JOIN)
            // Vous pouvez ajuster `parserConditionsJoin` pour prendre en compte les alias
            Condition[][] conditionsJoin = parserConditionsJoin(conditionJoin, table1, table2);

            // Appliquer la jointure gauche (LEFT JOIN)
            Relation resultat = table1.rightJoin(table2, conditionsJoin);

            // Étape 4 : Appliquer la clause WHERE si elle existe
            if (conditionsWhereStr != null) {
                Condition[][] conditionsWhere = parserConditions(conditionsWhereStr, resultat);
                resultat = resultat.selection(conditionsWhere);
            }

            // Étape 5 : Appliquer la projection (SELECT)
            if (!colonnesStr.equals("*")) {
                String[] colonnes = colonnesStr.split(",");
                ArrayList<String> nomsColonnes = new ArrayList<>();
                for (String colonne : colonnes) {
                    nomsColonnes.add(colonne.trim());
                }
                if (!requete.contains("*")) {
                    resultat = resultat.projection(nomsColonnes);
                }
            }

            // Retourner la relation résultante
            return resultat;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du traitement de la requête SELECT avec LEFT JOIN.");
        }
    }

    public static Relation executerSelectAvecOuterJoin(String requete) throws Exception {
        try {
            // Étape 1 : Parse de la requête avec les alias des tables
            String pattern = "SELECT\\s+(.*?)\\s+FROM\\s+(\\w+)\\s+(\\w+)\\s+OUTER\\s+JOIN\\s+(\\w+)\\s+(\\w+)\\s+ON\\s+((?:\\w+\\.\\w+\\s*=\\s*\\w+\\.\\w+)(?:\\s+AND\\s+\\w+\\.\\w+\\s*=\\s*\\w+\\.\\w+)*)\\s*(WHERE\\s+(.*))?";
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(requete);

            if (!matcher.find()) {
                throw new IllegalArgumentException(
                        "Erreur : Syntaxe incorrecte pour la requête SELECT avec LEFT JOIN.");
            }

            // Extraire les différentes parties de la requête
            String colonnesStr = matcher.group(1).trim(); // Colonnes à sélectionner
            String nomTable1 = matcher.group(2).trim(); // Première table
            String aliasTable1 = matcher.group(3).trim(); // Alias de la première table
            String nomTable2 = matcher.group(4).trim(); // Deuxième table (table à joindre)
            String aliasTable2 = matcher.group(5).trim(); // Alias de la deuxième table
            String conditionJoin = matcher.group(6).trim(); // Condition ON
            String conditionsWhereStr = matcher.group(8); // Clause WHERE (peut être null)

            // Debug : Affichage de la condition JOIN
            System.out.println("conditionJoin = " + conditionJoin);

            // Étape 2 : Charger les relations (tables)
            Relation table1 = chargerRelation(obtenirBaseActive(), nomTable1);
            Relation table2 = chargerRelation(obtenirBaseActive(), nomTable2);

            if (table1 == null || table2 == null) {
                throw new IllegalArgumentException("Erreur : Une ou plusieurs tables spécifiées n'existent pas.");
            }

            // Étape 3 : Créer les conditions de jointure (LEFT JOIN)
            // Vous pouvez ajuster `parserConditionsJoin` pour prendre en compte les alias
            Condition[][] conditionsJoin = parserConditionsJoin(conditionJoin, table1, table2);

            // Appliquer la jointure gauche (LEFT JOIN)
            Relation resultat = table1.joinExterne(table2, conditionsJoin);

            // Étape 4 : Appliquer la clause WHERE si elle existe
            if (conditionsWhereStr != null) {
                Condition[][] conditionsWhere = parserConditions(conditionsWhereStr, resultat);
                resultat = resultat.selection(conditionsWhere);
            }

            // Étape 5 : Appliquer la projection (SELECT)
            if (!colonnesStr.equals("*")) {
                String[] colonnes = colonnesStr.split(",");
                ArrayList<String> nomsColonnes = new ArrayList<>();
                for (String colonne : colonnes) {
                    nomsColonnes.add(colonne.trim());
                }
                if (!requete.contains("*")) {
                    resultat = resultat.projection(nomsColonnes);
                }
            }

            // Retourner la relation résultante
            return resultat;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du traitement de la requête SELECT avec LEFT JOIN.");
        }
    }

    public static Condition[][] parserConditionsJoinExterne(String conditionJoin, Relation relation1,
            Relation relation2) throws Exception {
        // Nettoyage des espaces
        conditionJoin = conditionJoin.trim();

        // Séparer la condition en attributs à comparer
        String[] conditions = conditionJoin.split("=");
        if (conditions.length != 2) {
            throw new IllegalArgumentException("Syntaxe de condition JOIN incorrecte : " + conditionJoin);
        }

        // Extraire les noms des attributs des tables
        String attribut1 = conditions[0].trim(); // Par exemple, "p1.nom"
        String attribut2 = conditions[1].trim(); // Par exemple, "p2.nom1"

        // Enlever les alias de table et récupérer les noms d'attributs
        String nomAttribut1 = extraireAttributSansAlias(attribut1);
        String nomAttribut2 = extraireAttributSansAlias(attribut2);

        // Vérifier si ces attributs existent dans les deux relations
        Attribut attributRelation1 = relation1.getAttributParNom(nomAttribut1);
        Attribut attributRelation2 = relation2.getAttributParNom(nomAttribut2);

        if (attributRelation1 == null || attributRelation2 == null) {
            throw new IllegalArgumentException("Les attributs spécifiés dans la condition JOIN sont invalides.");
        }

        // Créer la condition avec les attributs extraits
        Condition condition = new Condition(attributRelation1, attributRelation2, "=", relation1, relation2);

        // Retourner la condition sous forme de tableau à deux dimensions
        return new Condition[][] { { condition } };
    }

    public static String extraireAttributSansAlias(String attributAvecAlias) {
        // Séparer l'attribut de son alias (par exemple "p1.nom" -> "nom")
        String[] parts = attributAvecAlias.split("\\.");
        return parts[1].trim();
    }

    public static String showDatabases() {
        try {
            // Lire le fichier Data.txt pour extraire toutes les bases de données
            BufferedReader reader = new BufferedReader(new FileReader("Data.txt"));
            StringBuilder result = new StringBuilder();
            String ligne;

            // Parcourir chaque ligne du fichier pour trouver les bases de données
            while ((ligne = reader.readLine()) != null) {
                if (ligne.startsWith("Base: ")) {
                    result.append(ligne.substring(6).trim()).append("\n");
                }
            }

            reader.close();
            return result.toString().isEmpty() ? "Aucune base de données trouvée." : result.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return "Erreur lors de la lecture du fichier des bases de données.";
        }
    }

    // Fonction pour traiter la requête DESC
    public static String descTable(String requete) {
        try {
            // Étape 1 : Extraire le nom de la table de la requête DESC
            String pattern = "DESC\\s+(\\w+)"; // Le nom de la table après DESC
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regex.matcher(requete);

            if (!matcher.find()) {
                throw new IllegalArgumentException("Erreur : Syntaxe incorrecte pour la requête DESC.");
            }

            // Extraire le nom de la table
            String nomTable = matcher.group(1).trim();

            // Étape 2 : Charger la relation (table)
            Relation table = chargerRelation(obtenirBaseActive(), nomTable);

            if (table == null) {
                return "Erreur : La table '" + nomTable + "' n'existe pas.";
            }

            // Étape 3 : Construire la description de la table
            StringBuilder description = new StringBuilder();
            description.append("Table: ").append(nomTable).append("\n");

            // Affichage des attributs (colonnes) de la table
            description.append("Attributs: \n");
            for (Attribut attribut : table.getAttributs()) {
                description.append(" - ").append(attribut.getNom()).append(" {").append(attribut.getDomaine())
                        .append("}\n");
            }

            // Affichage des lignes de la table
            description.append("Lignes: \n");
            for (Ligne ligne : table.getLignes()) {
                Object[] elements = ligne.getElements();
                description.append(" - ").append(Arrays.toString(elements)).append("\n");
            }

            // Retourner la description sous forme de chaîne
            return description.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors du traitement de la requête DESC.";
        }
    }

    public static Relation executerDelete(String requete) throws Exception {
        // Étape 1 : Parse de la requête
        String pattern = "DELETE\\s+FROM\\s+(\\w+)\\s+(WHERE\\s+(.*))?";
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = regex.matcher(requete);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Erreur : Syntaxe incorrecte pour la requête DELETE.");
        }

        String nomTable = matcher.group(1).trim(); // Nom de la table
        String conditionsWhereStr = matcher.group(3); // Clause WHERE (peut être null)

        // Étape 2 : Charger la table
        Relation table = chargerRelation(obtenirBaseActive(), nomTable);
        if (table == null) {
            throw new IllegalArgumentException("Erreur : La table '" + nomTable + "' n'existe pas.");
        }

        // Étape 3 : Appliquer la clause WHERE si elle existe
        Relation lignesASupprimer = table; // Par défaut, toutes les lignes
        if (conditionsWhereStr != null) {
            Condition[][] conditions = parserConditions(conditionsWhereStr, table);
            lignesASupprimer = lignesASupprimer.selection(conditions);
        }
        Relation retour = Relation.difference(table, lignesASupprimer);
        retour.setNom(table.getNom());
        // Retourner la relation des lignes à supprimer
        mettreAJourFichier(retour);
        return retour;
    }

    private static void mettreAJourFichier(Relation table) throws IOException {
        String nomBase = obtenirBaseActive(); // Obtenir la base active
        if (nomBase == null) {
            throw new IllegalStateException("Erreur : Aucune base active définie.");
        }

        File inputFile = new File("Data.txt");
        File tempFile = new File("Data_temp.txt");

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String ligne;
            boolean baseTrouvee = false;
            boolean tableTrouvee = false;

            while ((ligne = reader.readLine()) != null) {
                // Repérer la base active
                if (ligne.equals("Base: " + nomBase)) {
                    baseTrouvee = true;
                }

                // Si la base active est trouvée
                if (baseTrouvee) {
                    // Identifier la table cible
                    if (ligne.startsWith("  - Table: " + table.getNom())) {
                        tableTrouvee = true;

                        // Ignorer les anciennes données de la table
                        while ((ligne = reader.readLine()) != null && !ligne.startsWith("  - Table: ")
                                && !ligne.startsWith("Base: ")) {
                            // Ignorer les anciennes lignes de la table
                        }

                        // Écrire la nouvelle version de la table
                        writer.write(serializeTable(table));
                        writer.newLine();

                        // Si on a atteint une nouvelle base ou table, ne pas ignorer ces lignes
                        if (ligne != null && (ligne.startsWith("Base: ") || ligne.startsWith("  - Table: "))) {
                            writer.write(ligne);
                            writer.newLine();
                        }
                        continue;
                    }
                }

                // Copier les autres lignes
                writer.write(ligne);
                writer.newLine();
            }

            // Si la table n'a pas été écrite, l'ajouter à la fin
            if (baseTrouvee && !tableTrouvee) {
                writer.write(serializeTable(table));
                writer.newLine();
            }
        }

        // Remplacer l'ancien fichier par le fichier temporaire
        if (!inputFile.delete() || !tempFile.renameTo(inputFile)) {
            throw new IOException("Erreur lors de la mise à jour du fichier Data.txt.");
        }
    }

    public static void executerUpdate(String requete) throws Exception {
        // Variables pour stocker les différentes parties de la requête
        String nomTable = "";
        ArrayList<String> attributs = new ArrayList<>();
        ArrayList<String> valeurs = new ArrayList<>();
        String condition = "";

        // Expression régulière pour parser la requête UPDATE
        String pattern = "UPDATE\\s+(\\w+)\\s+SET\\s+((?:\\w+\\s*=\\s*[^,\\s]+\\s*,\\s*)*\\w+\\s*=\\s*[^,\\s]+)\\s+WHERE\\s+(.+)";
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = regex.matcher(requete);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Erreur : Syntaxe incorrecte pour la requête UPDATE.");
        }

        // Extraire les différentes parties de la requête
        nomTable = matcher.group(1).trim(); // Partie 1 : La table
        String setClause = matcher.group(2).trim(); // Partie 2 : La liste des attributs et valeurs
        condition = matcher.group(3).trim(); // Partie 4 : La condition après WHERE

        // Séparer les attributs et les valeurs
        String[] setParts = setClause.split("\\s*,\\s*");
        for (String part : setParts) {
            String[] keyValue = part.split("\\s*=\\s*");
            attributs.add(keyValue[0].trim());
            valeurs.add(keyValue[1].trim());
        }

        // Afficher les résultats pour le débogage
        System.out.println("Table: " + nomTable);
        System.out.println("Attributs: " + attributs);
        System.out.println("Valeurs: " + valeurs);
        System.out.println("Condition: " + condition);

        Relation relation = chargerRelation(obtenirBaseActive(), nomTable);
        Condition[][] conditions = parserConditions(condition, relation);
        Relation delete = relation.selection(conditions);
        ArrayList<Ligne> lignes = delete.getLignes();
        Relation retour = Relation.difference(relation, delete);
        if(delete.getLignes().size() == 0)
        {
            throw new Exception("Aucune ligne ne correspond à la condition");
        }
        retour.setNom(nomTable);

        for (int i = 0; i < attributs.size(); i++) {
            for (int j = 0; j < relation.getAttributs().length; j++) {
                if (relation.getAttributs()[j].getNom().equals(attributs.get(i))) {
                    Domaine domaine = retour.getAttributParNom(attributs.get(i)).getDomaine();
                    for (int k = 0; k < lignes.size(); k++) {
                        if (domaine instanceof DomaineFini) {
                            lignes.get(k).setElement(j, convertirValeurSimple(valeurs.get(i)));
                        } else if (domaine instanceof DomaineInfini) {
                            lignes.get(k).setElement(j, convertirValeur(((DomaineInfini) domaine), valeurs.get(i)));
                        } else if (domaine instanceof DomaineMixte) {
                            lignes.get(k).setElement(j, convertirValeur(((DomaineMixte) domaine), valeurs.get(i)));
                        }
                        retour.ajouterLigne(lignes.get(k));
                    }
                }
            }
        }
        retour.afficherRelation();
        mettreAJourFichier(retour);
    }
}
