package aff;

import condition.Condition;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import domaine.*;
import rlt.*;
import condition.*;
public class Main {
    public static void main(String[] args) {
        // Création des éléments pour les domaines
        Set<Object> elementsFini = new HashSet<>();
        elementsFini.add(25);
        elementsFini.add(30);
        elementsFini.add(35);

        List<Class<?>> typesInfini = new ArrayList<>();
        typesInfini.add(Integer.class);  // Domaine infini pour Integer
        typesInfini.add(String.class);   // Domaine infini pour String

        // Création des domaines
        DomaineFini domaineFini = new DomaineFini(elementsFini);
        DomaineInfini domaineInfini = new DomaineInfini(typesInfini);
        DomaineMixte domaineMixte = new DomaineMixte(typesInfini, elementsFini);

        // Création des attributs pour les relations
        Attribut[] attributs1 = {
            new Attribut(domaineInfini, "ID1"),
            new Attribut(domaineInfini, "Nom"),
            new Attribut(domaineFini, "Age")
        };

        Attribut[] attributs2 = {
            new Attribut(domaineInfini, "ID2"),
            new Attribut(domaineInfini, "Ville")
        };

        // Création des relations
        Relation relation1 = new Relation("Personnes", attributs1);
        relation1.ajouterLigne(new Ligne(new Object[]{1, "Alice", 25}));
        relation1.ajouterLigne(new Ligne(new Object[]{2, "Bob", 30}));
        relation1.ajouterLigne(new Ligne(new Object[]{3, "Charlie", 35}));

        relation1.afficherRelation();

        Relation relation2 = new Relation("Residences", attributs2);
        relation2.ajouterLigne(new Ligne(new Object[]{1, "Paris"}));
        relation2.ajouterLigne(new Ligne(new Object[]{2, "Lyon"}));
        relation2.ajouterLigne(new Ligne(new Object[]{4, "Marseille"}));

        // Affichage initial des relations
        System.out.println("=== Relation 1 (Personnes) ===");
        relation1.afficherRelation();

        System.out.println("\n=== Relation 2 (Residences) ===");
        relation2.afficherRelation();

        // Test de la projection
        System.out.println("\n=== Projection (Nom, Age) de Relation 1 ===");
        ArrayList<String> nomsAttributs = new ArrayList<>();
        nomsAttributs.add(attributs1[1].getNom()); // Par exemple "Nom"
        nomsAttributs.add(attributs1[2].getNom()); // Par exemple "Age"
        Relation projection = relation1.projection(nomsAttributs);
        projection.afficherRelation();

        // Test de la sélection
        System.out.println("\n=== Sélection (Age > 30) sur Relation 1 ===");
        Condition conditionSelection1 = new Condition(30, ">", attributs1[2], relation1);
        Condition conditionSelection2 = new Condition(30, "=", attributs1[2], relation1);
        Condition[][] conditions = new Condition[][] { { conditionSelection1},{conditionSelection2} };
        Relation selection = relation1.selection(conditions);
        selection.afficherRelation();

        // Test du produit cartésien
        System.out.println("\n=== Produit Cartésien de Relation 1 et Relation 2 ===");
        Relation produitCartesien = Relation.produitCartesien(relation1, relation2);
        produitCartesien.afficherRelation();

        // Test de la theta jointure
        System.out.println("\n=== Theta Jointure (Personnes.ID = Residences.ID) ===");
        Condition conditionJointure = new Condition(attributs1[0], attributs2[0], "<", relation1, relation2);
        Relation thetaJointure = relation1.thetaJointure(relation2, new Condition[][] { { conditionJointure } });
        thetaJointure.afficherRelation();

        // Test de leftJoin
        System.out.println("\n=== LeftJoin (Personnes.ID = Residences.ID) ===");
        Condition conditionleftJoin = new Condition(attributs1[0], attributs2[0], "=", relation1, relation2);
        Relation leftjoin= relation1.leftJoin(relation2, new Condition[][] { { conditionleftJoin } });
        leftjoin.afficherRelation();

        // Test de rightJoin
        System.out.println("\n=== rightJoin (Personnes.ID = Residences.ID) ===");
        Condition conditionrightJoin = new Condition(attributs1[0], attributs2[0], "=", relation1, relation2);
        Relation rightjoin= relation1.rightJoin(relation2, new Condition[][] { { conditionrightJoin } });
        rightjoin.afficherRelation();

         // Test de joinExterne
         System.out.println("\n=== JoinExterne (Personnes.ID = Residences.ID) ===");
         Condition conditionjoinExterne = new Condition(attributs1[0], attributs2[0], ">", relation1, relation2);
         Relation joinExterne= relation1.joinExterne(relation2, new Condition[][] { { conditionjoinExterne } });
         joinExterne.afficherRelation();
    }
}
