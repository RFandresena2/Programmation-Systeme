package rlt;

import domaine.*;
import condition.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Relation {
    Attribut[] attributs;
    ArrayList<Ligne> lignes;
    String nom;

    // Constructeur pour initialiser les attributs et la liste des lignes
    public Relation() {
        this.attributs = null;
        this.lignes = null;
    }

    public Relation(String nom, Attribut[] attributs) {
        this.nom = nom;
        this.attributs = attributs;
        this.lignes = new ArrayList<>();
    }

    public Relation(Attribut[] attributs) {
        this.nom = "";
        this.attributs = attributs;
        this.lignes = new ArrayList<>();
    }

    public Relation(Attribut[] attributs, ArrayList<Ligne> lignes) {
        this.attributs = attributs;
        this.lignes = lignes;
    }

    // Getters
    public Attribut[] getAttributs() {
        return attributs;
    }

    public ArrayList<Ligne> getLignes() {
        return lignes;
    }

    // Setters
    public void setAttributs(Attribut[] attributs) {
        this.attributs = attributs;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    // Setter pour les lignes, avec vérification des conditions
    public void setLignes(ArrayList<Ligne> lignes) throws IllegalArgumentException {
        for (Ligne ligne : lignes) {
            verifierLigne(ligne);
        }
        this.lignes = new ArrayList<>(lignes);
    }

    // Méthode pour ajouter une seule ligne avec vérifications
    public void ajouterLigne(Ligne ligne) throws IllegalArgumentException {
        verifierLigne(ligne);
        verifierDoublonsLignes(ligne);
        lignes.add(ligne);
    }

    public void verifierDoublonsLignes(Ligne ligne) throws IllegalArgumentException {
        Object[] elements = ligne.getElements();
        // Vérification si la ligne existe déjà dans la relation
        for (Ligne ligneExistante : lignes) {
            if (Arrays.equals(ligneExistante.getElements(), elements)) {
                throw new IllegalArgumentException("La ligne existe déjà dans la relation.");
            }
        }
    }

    // Méthode pour vérifier la conformité d'une ligne
    public void verifierLigne(Ligne ligne) throws IllegalArgumentException {
        Object[] elements = ligne.getElements();

        // Vérification du nombre d'éléments
        if (elements.length != attributs.length) {
            throw new IllegalArgumentException(
                    "Le nombre d'éléments dans la ligne ne correspond pas au nombre d'attributs.");
        }

        // Vérification du type des éléments en fonction du domaine
        for (int i = 0; i < elements.length; i++) {
            Domaine domaine = attributs[i].getDomaine();
            Object element = elements[i];

            if (domaine instanceof DomaineFini) {
                // Vérifier si l'élément appartient au domaine fini
                System.out.println("element verifier = "+ element);
                System.out.println("instance "+ element.getClass().getSimpleName());
                if (element.toString().trim().equals("null")) {

                } else {

                    if (!((DomaineFini) domaine).appartient(element)) {
                        throw new IllegalArgumentException(
                                "L'élément à l'index " + i + " n'appartient pas au domaine fini de l'attribut "
                                        + attributs[i].getNom());
                    }
                }
            } else if (domaine instanceof DomaineInfini) {
                // Vérifier si l'élément est une instance des types du domaine infini
                if (!((DomaineInfini) domaine).appartient(element)) {
                    throw new IllegalArgumentException(
                            "L'élément à l'index " + i + " n'est pas compatible avec le domaine infini de l'attribut "
                                    + attributs[i].getNom());
                }
            } else if (domaine instanceof DomaineMixte) {
                // Vérifier si l'élément appartient au domaine mixte
                if (!((DomaineMixte) domaine).appartient(element)) {
                    throw new IllegalArgumentException(
                            "L'élément à l'index " + i + " n'appartient pas au domaine mixte de l'attribut "
                                    + attributs[i].getNom());
                }
            } else {
                // Si le domaine n'est pas reconnu
                throw new IllegalArgumentException(
                        "Le domaine de l'attribut " + attributs[i].getNom() + " n'est pas pris en charge.");
            }
        }
    }

    public Attribut getAttributParNom(String nomAttribut) {
        for (Attribut attribut : attributs) {
            if (attribut.getNom().equals(nomAttribut)) {
                return attribut; // Retourne l'attribut si le nom correspond
            }
        }
        return null; // Retourne null si l'attribut n'est pas trouvé
    }

    // Méthode pour afficher les informations de la relation
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Relation{ attributs=");
        sb.append(Arrays.toString(attributs)).append(", lignes=\n");

        for (Ligne ligne : lignes) {
            sb.append(ligne).append("\n");
        }

        sb.append("}");
        return sb.toString();
    }

    public static Relation union(Relation relation1, Relation relation2) throws IllegalArgumentException {
        // Vérification : les deux relations doivent avoir le même nombre d'attributs
        if (relation1.getAttributs().length != relation2.getAttributs().length) {
            throw new IllegalArgumentException("Les deux relations doivent avoir le même nombre d'attributs.");
        }

        // Combinaison des attributs
        Attribut[] nouveauxAttributs = new Attribut[relation1.getAttributs().length];
        for (int i = 0; i < nouveauxAttributs.length; i++) {
            Attribut attribut1 = relation1.getAttributs()[i];
            Attribut attribut2 = relation2.getAttributs()[i];

            // Combinaison des domaines
            Domaine nouveauDomaine;
            if (attribut1.getDomaine() instanceof DomaineFini && attribut2.getDomaine() instanceof DomaineFini) {
                // Union des éléments des domaines finis
                Set<Object> unionElements = new HashSet<>(((DomaineFini) attribut1.getDomaine()).getElements());
                unionElements.addAll(((DomaineFini) attribut2.getDomaine()).getElements());
                nouveauDomaine = new DomaineFini(unionElements);
            } else if (attribut1.getDomaine() instanceof DomaineInfini
                    && attribut2.getDomaine() instanceof DomaineInfini) {
                // Union des types des deux DomaineInfini
                ArrayList<Class<?>> unionTypes = new ArrayList<>(((DomaineInfini) attribut1.getDomaine()).getTypes());
                for (Class<?> type : ((DomaineInfini) attribut2.getDomaine()).getTypes()) {
                    if (!unionTypes.contains(type)) {
                        unionTypes.add(type);
                    }
                }
                nouveauDomaine = new DomaineInfini(unionTypes);
            } else if (attribut1.getDomaine() instanceof DomaineMixte
                    || attribut2.getDomaine() instanceof DomaineMixte) {
                // Combinaison pour DomaineMixte
                Set<Object> unionElements = new HashSet<>();
                ArrayList<Class<?>> unionTypes = new ArrayList<>();

                if (attribut1.getDomaine() instanceof DomaineMixte) {
                    DomaineMixte mixte1 = (DomaineMixte) attribut1.getDomaine();
                    unionElements.addAll(mixte1.getElements());
                    for (Class<?> type : mixte1.getTypes()) {
                        if (!unionTypes.contains(type)) {
                            unionTypes.add(type);
                        }
                    }
                }

                if (attribut2.getDomaine() instanceof DomaineMixte) {
                    DomaineMixte mixte2 = (DomaineMixte) attribut2.getDomaine();
                    unionElements.addAll(mixte2.getElements());
                    for (Class<?> type : mixte2.getTypes()) {
                        if (!unionTypes.contains(type)) {
                            unionTypes.add(type);
                        }
                    }
                }

                nouveauDomaine = new DomaineMixte(unionTypes, unionElements);
            } else {
                throw new IllegalArgumentException("Les domaines des attributs ne sont pas compatibles.");
            }

            // Création du nouvel attribut avec le domaine combiné
            nouveauxAttributs[i] = new Attribut(nouveauDomaine, attribut1.getNom());
        }

        // Combinaison des lignes
        // Création d'une nouvelle liste mutable
        ArrayList<Ligne> nouvellesLignes = new ArrayList<>();
        // Ajout des lignes de la première relation
        nouvellesLignes.addAll(relation1.getLignes());
        // Ajout des lignes de la deuxième relation
        nouvellesLignes.addAll(relation2.getLignes());

        // Création de la nouvelle relation
        return new Relation(nouveauxAttributs, nouvellesLignes);
    }

    // Méthode d'intersection
    public static Relation intersection(Relation relation1, Relation relation2) throws IllegalArgumentException {
        // Vérification : les deux relations doivent avoir le même nombre d'attributs
        if (relation1.getAttributs().length != relation2.getAttributs().length) {
            throw new IllegalArgumentException("Les deux relations doivent avoir le même nombre d'attributs.");
        }

        // Vérification : les domaines des attributs doivent correspondre
        Attribut[] attributs1 = relation1.getAttributs();
        Attribut[] attributs2 = relation2.getAttributs();
        for (int i = 0; i < attributs1.length; i++) {
            if (!attributs1[i].getDomaine().equals(attributs2[i].getDomaine())) {
                throw new IllegalArgumentException("Les domaines des attributs doivent être identiques.");
            }
        }

        // Intersection des lignes
        ArrayList<Ligne> nouvellesLignes = new ArrayList<>();
        for (Ligne ligne1 : relation1.getLignes()) {
            for (Ligne ligne2 : relation2.getLignes()) {
                if (ligne1.equals(ligne2)) {
                    nouvellesLignes.add(ligne1);
                }
            }
        }

        // Création de la nouvelle relation avec les lignes communes
        return new Relation(attributs1, nouvellesLignes);
    }

    // Projection : crée une nouvelle relation avec des attributs sélectionnés
    public Relation projection(ArrayList<String> nomsAttributs) {
        ArrayList<Attribut> nouveauxAttributs = new ArrayList<>();
        ArrayList<Ligne> nouvellesLignes = new ArrayList<>();

        // Trouver les attributs correspondant aux noms donnés
        for (String nom : nomsAttributs) {
            for (Attribut attribut : attributs) {
                if (attribut.getNom().equals(nom)) {
                    nouveauxAttributs.add(attribut);
                }
            }
        }

        // Ajouter uniquement les valeurs des attributs sélectionnés pour chaque ligne
        for (Ligne ligne : lignes) {
            ArrayList<Object> elementsProjetes = new ArrayList<>();
            for (Attribut attribut : nouveauxAttributs) {
                for (int i = 0; i < attributs.length; i++) {
                    if (attributs[i].getNom().equals(attribut.getNom())) {
                        elementsProjetes.add(ligne.getElements()[i]);
                    }
                }
            }
            nouvellesLignes.add(new Ligne(elementsProjetes.toArray()));
        }

        // Retourner une nouvelle relation projetée
        return new Relation(nouveauxAttributs.toArray(new Attribut[0]), nouvellesLignes);
    }

    public Relation selection(Condition[]... tableauxDeConditions) {
        Relation resultatFinal = null; // Contiendra la relation finale après application de "ou"

        // Parcours de chaque tableau de conditions
        for (Condition[] conditions : tableauxDeConditions) {
            // Application de "et" sur les conditions dans le tableau courant
            Relation resultatEt = Condition.et(conditions);

            // Application de "ou" entre les relations obtenues
            if (resultatFinal == null) {
                // Initialisation de la relation finale avec le premier résultat
                resultatFinal = resultatEt;
            } else {
                // Union avec les résultats des autres tableaux
                resultatFinal = Relation.union(resultatFinal, resultatEt);
            }
        }
        resultatFinal.supprimerDoublons();
        // Si aucun tableau de conditions n'a produit de résultats, retourner une
        // relation vide
        return resultatFinal != null ? resultatFinal : new Relation(this.getNom(), this.getAttributs());
    }

    public Relation thetaJointure(Relation autreRelation, Condition[]... tableauxDeConditions) {
        Relation retour = new Relation();
        String attribut1_original = "";
        String attribut2_original = "";
        retour = Relation.produitCartesien(this, autreRelation);
        for (Condition[] tableau : tableauxDeConditions) {
            for (Condition condition : tableau) {
                condition.setRelation2(null);
                condition.setRelation1(retour);
            }
        }
        // System.out.println("avant");
        // retour.afficherRelation();
        if (tableauxDeConditions.length == 1) {
            if (tableauxDeConditions[0][0].getAttribut().getNom()
                    .equals(tableauxDeConditions[0][0].getAttribut2().getNom())) {
                System.out.println("a1 = " + attribut1_original);
                System.out.println("a2 = " + attribut2_original);
                retour.afficherRelation();
            }
        }
        retour = retour.selection(tableauxDeConditions);
        return retour;
    }

    public static Relation produitCartesien(Relation relation1, Relation relation2) {
        modifierNomColonne(relation1,relation2);
        // Combinaison des attributs des deux relations
        ArrayList<Attribut> attributsCombines = new ArrayList<>(Arrays.asList(relation1.getAttributs()));
        attributsCombines.addAll(Arrays.asList(relation2.getAttributs()));

        // Fusion des lignes
        ArrayList<Ligne> lignesCombinees = new ArrayList<>();

        for (Ligne ligne1 : relation1.getLignes()) {
            for (Ligne ligne2 : relation2.getLignes()) {
                // Combiner les éléments des deux lignes
                Object[] elementsCombines = new Object[ligne1.getElements().length + ligne2.getElements().length];
                System.arraycopy(ligne1.getElements(), 0, elementsCombines, 0, ligne1.getElements().length);
                System.arraycopy(ligne2.getElements(), 0, elementsCombines, ligne1.getElements().length,
                        ligne2.getElements().length);

                // Vérifier que tous les éléments respectent les domaines combinés
                boolean valide = true;
                for (int i = 0; i < attributsCombines.size(); i++) {
                    if (!attributsCombines.get(i).getDomaine().appartient(elementsCombines[i])) {
                        valide = false;
                        break;
                    }
                }

                if (valide) {
                    lignesCombinees.add(new Ligne(elementsCombines));
                }
            }
        }

        // Retourner la nouvelle relation avec les attributs et lignes combinés
        Relation retour = new Relation(attributsCombines.toArray(new Attribut[0]), lignesCombinees);
        // retour.afficherRelation();
        return retour;
    }

    public static void modifierNomColonne(Relation r1, Relation r2)
    {
        for (int i = 0; i < r1.getAttributs().length; i++) {
            r1.getAttributs()[i].setNom(r1.getNom().concat(".").concat(r1.getAttributs()[i].getNom()));
        }
        for (int i = 0; i < r2.getAttributs().length; i++) {
            r2.getAttributs()[i].setNom(r2.getNom().concat(".").concat(r2.getAttributs()[i].getNom()));
        }
    }

    public Relation leftJoin(Relation right, Condition[]... tableauxDeConditions) {
        Relation retour = this.thetaJointure(right, tableauxDeConditions);
        ArrayList<Integer> manquant = Relation.getIndiceLigneManquant(this, retour);
        if (manquant.size() > 0) {
            retour.getLignes().addAll(Relation.remplirParNull(this, manquant, retour));
        }
        return retour;
    }

    public Relation rightJoin(Relation right, Condition[]... tableauxDeConditions) {
        Relation retour = this.thetaJointure(right, tableauxDeConditions);
        ArrayList<Integer> manquant = Relation.getIndiceLigneManquant(right, retour);
        if (manquant.size() > 0) {
            retour.getLignes().addAll(Relation.remplirParNull(right, manquant, retour));
        }
        return retour;
    }

    public Relation joinExterne(Relation right, Condition[]... tableauxDeConditions) {
        Relation retour = leftJoin(right, tableauxDeConditions);
        retour.getLignes().addAll(rightJoin(right, tableauxDeConditions).getLignes());
        retour.supprimerDoublons();
        return retour;
    }

    public static Relation difference(Relation relation1, Relation relation2) {
        // Vérification des conditions préalables
        if (relation1.getAttributs().length != relation2.getAttributs().length) {
            throw new IllegalArgumentException("Les deux relations doivent avoir le même nombre d'attributs.");
        }

        // Vérifier que tous les domaines des attributs correspondent
        Attribut[] attributs1 = relation1.getAttributs();
        Attribut[] attributs2 = relation2.getAttributs();
        for (int i = 0; i < attributs1.length; i++) {
            if (!attributs1[i].getDomaine().equals(attributs2[i].getDomaine())) {
                throw new IllegalArgumentException(
                        "Les domaines des attributs correspondants doivent être identiques.");
            }
        }

        // Trouver les lignes présentes dans la première relation mais pas dans la
        // seconde
        ArrayList<Ligne> lignesResultantes = new ArrayList<>();

        for (Ligne ligne1 : relation1.getLignes()) {
            boolean estUnique = true;
            for (Ligne ligne2 : relation2.getLignes()) {
                if (ligne1.equals(ligne2)) {
                    estUnique = false;
                    break;
                }
            }
            if (estUnique) {
                lignesResultantes.add(ligne1);
            }
        }

        // Retourner la nouvelle relation avec les lignes résultantes et les mêmes
        // attributs
        return new Relation(attributs1, lignesResultantes);
    }

    public void afficherRelation() {
        // Obtenir le nombre d'attributs dans la relation
        int nombreAttributs = this.getAttributs().length;

        // Calculer la largeur maximale nécessaire pour chaque colonne
        int[] largeursColonnes = new int[nombreAttributs];

        for (int i = 0; i < nombreAttributs; i++) {
            // Initialiser la largeur de la colonne avec la longueur du nom de l'attribut
            largeursColonnes[i] = this.getAttributs()[i].getNom().length();

            // Vérifier les éléments de chaque ligne pour ajuster la largeur
            for (Ligne ligne : lignes) {
                Object[] elements = ligne.getElements();
                if (i < elements.length) {
                    largeursColonnes[i] = Math.max(largeursColonnes[i],
                            elements[i] != null ? elements[i].toString().length() : 4); // 4 est la longueur de "null"
                }
            }
        }

        // Afficher la ligne de séparation supérieure
        afficherLigneSeparation(largeursColonnes);

        // Afficher les en-têtes des colonnes
        System.out.print("|");
        for (int i = 0; i < nombreAttributs; i++) {
            System.out.printf(" %-" + largeursColonnes[i] + "s |", this.getAttributs()[i].getNom());
        }
        System.out.println();

        // Afficher la ligne de séparation sous les en-têtes
        afficherLigneSeparation(largeursColonnes);

        // Afficher les données
        for (Ligne ligne : lignes) {
            System.out.print("|");
            Object[] elements = ligne.getElements();
            for (int i = 0; i < nombreAttributs; i++) {
                // Gérer les éléments manquants ou null
                String valeur = (i < elements.length && elements[i] != null) ? elements[i].toString() : "null";
                System.out.printf(" %-" + largeursColonnes[i] + "s |", valeur);
            }
            System.out.println();
        }

        // Afficher la ligne de séparation inférieure
        afficherLigneSeparation(largeursColonnes);
    }

    // Méthode utilitaire pour afficher une ligne de séparation
    private void afficherLigneSeparation(int[] largeursColonnes) {
        System.out.print("+");
        for (int largeur : largeursColonnes) {
            System.out.print("-".repeat(largeur + 2) + "+");
        }
        System.out.println();
    }

    public static ArrayList<Integer> getIndiceLigneManquant(Relation relation, Relation produit_cartésien) {
        ArrayList<Integer> retour = new ArrayList<>();
        if (produit_cartésien.getLignes().size() == 0) {
            for (int i = 0; i < relation.getLignes().size(); i++) {
                retour.add(i);
            }
            return retour;
        }
        int first_attribut_table = 0;
        for (int i = 0; i < produit_cartésien.getAttributs().length; i++) {
            if (produit_cartésien.getAttributs()[i].getNom().equals(relation.getAttributs()[0].getNom())) {
                first_attribut_table = i;
            }
        }
        // System.out.println("indice de l'attribut de la premiere colonne =
        // "+first_attribut_table);
        for (int j = 0; j < relation.getLignes().size(); j++) {
            int present = 0;
            for (int i = 0; i < produit_cartésien.getLignes().size(); i++) {
                if (produit_cartésien.getLignes().get(i).getElement(first_attribut_table).toString()
                        .equals(relation.getLignes().get(j).getElement(0).toString())) {
                    present++;
                }
            }
            if (present == 0) {
                // System.out.println("indice = "+ j);
                retour.add(j);
            }
        }
        // System.out.println("retour = "+ retour);
        return retour;
    }

    public static ArrayList<Ligne> remplirParNull(Relation relation, ArrayList<Integer> indice, Relation produit) {
        ArrayList<Ligne> retour = new ArrayList<>();
        int first_attribut_table = 0;
        for (int i = 0; i < produit.getAttributs().length; i++) {
            if (produit.getAttributs()[i].getNom().equals(relation.getAttributs()[0].getNom())) {
                first_attribut_table = i;
            }
        }
        // System.out.println("first = "+ first_attribut_table);
        for (int i = 0; i < indice.size(); i++) {
            Object[] elements = new Object[produit.getAttributs().length];
            int plus = 0;
            for (int j = 0; j < produit.getAttributs().length; j++) {
                // System.out.println("j = "+j);
                elements[j] = null;
                if (j >= first_attribut_table && j < (relation.getAttributs().length) + first_attribut_table) {
                    // System.out.println("mitsofoka");
                    elements[j] = relation.getLignes().get(indice.get(i)).getElement(plus);
                    plus++;
                }
            }
            retour.add(new Ligne(elements));
        }
        return retour;
    }

    public void supprimerDoublons() {
        ArrayList<Ligne> lignesSansDoublons = new ArrayList<>();

        for (Ligne ligne : lignes) {
            boolean estDoublon = false;
            // Comparer chaque ligne avec les autres déjà présentes dans la nouvelle liste
            for (Ligne ligneExistante : lignesSansDoublons) {
                if (ligne.toString().equals(ligneExistante.toString())) {
                    estDoublon = true; // Si la ligne existe déjà, c'est un doublon
                    break;
                }
            }
            if (!estDoublon) {
                lignesSansDoublons.add(ligne); // Ajouter la ligne si elle n'est pas un doublon
            }
        }

        // Remplacer les anciennes lignes par les lignes sans doublons
        lignes = lignesSansDoublons;
    }

    public String obtenirAffichageRelation() {
        StringBuilder affichage = new StringBuilder();

        // Obtenir le nombre d'attributs dans la relation
        int nombreAttributs = this.getAttributs().length;

        // Calculer la largeur maximale nécessaire pour chaque colonne
        int[] largeursColonnes = new int[nombreAttributs];

        for (int i = 0; i < nombreAttributs; i++) {
            // Initialiser la largeur de la colonne avec la longueur du nom de l'attribut
            largeursColonnes[i] = this.getAttributs()[i].getNom().length();

            // Vérifier les éléments de chaque ligne pour ajuster la largeur
            for (Ligne ligne : lignes) {
                Object[] elements = ligne.getElements();
                if (i < elements.length) {
                    largeursColonnes[i] = Math.max(largeursColonnes[i],
                            elements[i] != null ? elements[i].toString().length() : 4); // 4 est la longueur de "null"
                }
            }
        }

        // Ajouter la ligne de séparation supérieure
        affichage.append(construireLigneSeparation(largeursColonnes));

        // Ajouter les en-têtes des colonnes
        affichage.append("|");
        for (int i = 0; i < nombreAttributs; i++) {
            affichage.append(String.format(" %-" + largeursColonnes[i] + "s |", this.getAttributs()[i].getNom()));
        }
        affichage.append("\n");

        // Ajouter la ligne de séparation sous les en-têtes
        affichage.append(construireLigneSeparation(largeursColonnes));

        // Ajouter les données
        for (Ligne ligne : lignes) {
            affichage.append("|");
            Object[] elements = ligne.getElements();
            for (int i = 0; i < nombreAttributs; i++) {
                // Gérer les éléments manquants ou null
                String valeur = (i < elements.length && elements[i] != null) ? elements[i].toString() : "null";
                affichage.append(String.format(" %-" + largeursColonnes[i] + "s |", valeur));
            }
            affichage.append("\n");
        }

        // Ajouter la ligne de séparation inférieure
        affichage.append(construireLigneSeparation(largeursColonnes));

        return affichage.toString();
    }

    // Méthode utilitaire pour construire une ligne de séparation
    private String construireLigneSeparation(int[] largeursColonnes) {
        StringBuilder separation = new StringBuilder();
        separation.append("+");
        for (int largeur : largeursColonnes) {
            separation.append("-".repeat(largeur + 2)).append("+");
        }
        separation.append("\n");
        return separation.toString();
    }

}