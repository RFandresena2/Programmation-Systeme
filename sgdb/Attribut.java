package rlt;
import domaine.*;

public class Attribut {
    private Domaine domaine; // Remplace Class<?> par Domaine
    private String nom;

    // Constructeur
    public Attribut(Domaine domaine, String nom) {
        this.domaine = domaine;
        this.nom = nom;
    }

    // Getters
    public Domaine getDomaine() {
        return domaine;
    }

    public String getNom() {
        return nom;
    }

    // Setters
    public void setDomaine(Domaine domaine) {
        this.domaine = domaine;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    // Méthode pour vérifier si une valeur appartient au domaine de l'attribut
    public boolean appartientAuDomaine(Object valeur) {
        return domaine.appartient(valeur);
    }

    // Méthode pour afficher l'information de l'attribut
    @Override
public String toString() {
    // Génère une chaîne au format : new Attribut(new DomaineInfini(List.of(java.lang.Integer.class)), "id")
    return "new Attribut(" + domaine.toString() + ", \"" + nom + "\")";
}

}
