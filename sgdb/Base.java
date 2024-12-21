package rlt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Base {
    String nom;
    private ArrayList<Relation> relations;

    // Constructeur qui initialise une base vide
    public Base(String nom) {
        this.nom = nom;
        this.relations = new ArrayList<Relation>();
    }
    
    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    // Getter pour obtenir la liste des relations
    public ArrayList<Relation> getRelations() {
        return relations;
    }

    // Setter pour remplacer l'ensemble des relations
    public void setRelations(List<Relation> relations) {
        this.relations = new ArrayList<>(relations);
    }

    // Méthode pour ajouter une relation à la base
    public void ajouterRelation(Relation relation) {
        this.relations.add(relation);
    }

    // Méthode pour récupérer une relation par son nom (s'il y a un attribut nom
    // dans Relation)
    public Relation getRelationParNom(String nom) {
        System.out.println(this.getRelations());
        System.out.println("à comparer "+nom);
        for (Relation relation : relations) {
            // On suppose que chaque Relation a un nom (il pourrait être ajouté en tant
            // qu'attribut de Relation)
            System.out.println("nom relation "+relation.nom);
            if (relation.getNom().equals(nom)) { // Si on utilise un nom, il vaut mieux le spécifier explicitement
                                                     // dans Relation
                return relation;
            }
        }
        return null; // Retourne null si aucune relation avec ce nom n'est trouvée
    }

    // Méthode pour afficher les informations de la base
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Base{ relations=\n");
        for (Relation relation : relations) {
            sb.append(relation).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }


}
