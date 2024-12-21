package condition;

import rlt.*;

public class Condition {
    Object valeur; // Valeur à comparer avec l'attribut (remplaçant element1 et element2)
    String operateur; // L'opérateur de comparaison (ex : ">", "<", "=")
    Attribut attribut; // L'attribut de la première relation à comparer avec la valeur ou un autre
                       // attribut
    Attribut attribut2; // Deuxième attribut pour la comparaison entre deux colonnes
    Relation relation1; // La première relation associée à attribut
    Relation relation2; // La deuxième relation associée à attribut2

    // Constructeur pour comparer un attribut avec une valeur
    public Condition(Object valeur, String operateur, Attribut attribut, Relation relation1) {
        this.valeur = valeur;
        this.operateur = operateur;
        this.attribut = attribut;
        this.attribut2 = null; // Pas de deuxième attribut pour la comparaison
        this.relation1 = relation1;
        this.relation2 = null; // Pas de deuxième relation
    }

    // Constructeur pour comparer deux attributs (colonnes) entre deux relations
    public Condition(Attribut attribut1, Attribut attribut2, String operateur, Relation relation1, Relation relation2) {
        this.valeur = null; // Pas de valeur fixe
        this.operateur = operateur;
        this.attribut = attribut1;
        this.attribut2 = attribut2;
        this.relation1 = relation1;
        this.relation2 = relation2;
    }

    public Object getValeur() {
        return valeur;
    }

    public void setValeur(Object valeur) {
        this.valeur = valeur;
    }

    public String getOperateur() {
        return operateur;
    }

    public void setOperateur(String operateur) {
        this.operateur = operateur;
    }

    public Attribut getAttribut() {
        return attribut;
    }

    public void setAttribut(Attribut attribut) {
        this.attribut = attribut;
    }

    public Attribut getAttribut2() {
        return attribut2;
    }

    public void setAttribut2(Attribut attribut2) {
        this.attribut2 = attribut2;
    }

    public Relation getRelation1() {
        return relation1;
    }

    public void setRelation1(Relation relation1) {
        this.relation1 = relation1;
    }

    public Relation getRelation2() {
        return relation2;
    }

    public void setRelation2(Relation relation2) {
        this.relation2 = relation2;
    }

    // Méthode pour évaluer une seule condition
    public Relation evaluer() {
        // Déterminer la relation à utiliser (relation1 ou relation2 selon le cas)
        Relation relation = (this.relation2 == null) ? relation1 : relation2;
        Relation nouvelleRelation = new Relation(this.relation1.getAttributs());

        // Parcours des lignes de la relation
        for (Ligne ligne : relation.getLignes()) {
            // Récupérer la valeur de l'attribut à comparer
            Object valeur1 = ligne.getElement(obtenirPositionAttribut(this.attribut, relation.getAttributs()));

            // Si deux attributs sont comparés, obtenir la valeur de l'autre attribut
            Object valeur2 = (this.attribut2 != null)
                    ? ligne.getElement(obtenirPositionAttribut(this.attribut2, relation.getAttributs()))
                    : null;

            boolean conditionSatisfaite;

            // Si on compare deux colonnes (attributs)
            if (this.attribut2 != null) {
                conditionSatisfaite = comparerColonnes(valeur1, valeur2);
            } else {
                // Sinon, on fait la comparaison avec une valeur
                conditionSatisfaite = comparerAvecValeur(valeur1);
            }

            // Ajouter la ligne à la nouvelle relation si la condition est satisfaite
            if (conditionSatisfaite) {
                nouvelleRelation.ajouterLigne(ligne);
            }
        }

        return nouvelleRelation;
    }

    // Comparer les éléments de deux colonnes (attributs) pour chaque ligne
    private boolean comparerColonnes(Object valeur1, Object valeur2) {
        if (valeur1 == null || valeur2 == null) {
            return false;
        }

        // Effectuer la comparaison selon l'opérateur spécifié
        switch (operateur) {
            case "=":
                return egal(valeur1, valeur2);
            case "<":
                return inferieur(valeur1, valeur2);
            case ">":
                return superieur(valeur1, valeur2);
            default:
                throw new IllegalArgumentException(
                        "Opérateur non supporté pour la comparaison entre colonnes : " + operateur);
        }
    }

    // Comparer un attribut avec une valeur
    private boolean comparerAvecValeur(Object valeur1) {
        if (valeur1 == null || this.valeur == null) {
            return false;
        }

        // Effectuer la comparaison selon l'opérateur spécifié
        switch (operateur) {
            case "=":
                return egal(valeur1, valeur);
            case "<":
                return inferieur(valeur1, valeur);
            case ">":
                return superieur(valeur1, valeur);
            default:
                throw new IllegalArgumentException("Opérateur non supporté : " + operateur);
        }
    }

    // Les fonctions de comparaison restent inchangées
    public boolean egal(Object valeur1, Object valeur2) {
        if (valeur1 == null || valeur2 == null) {
            return false;
        }
        return valeur1.equals(valeur2);
    }

    public boolean inferieur(Object valeur1, Object valeur2) {
        if (valeur1 instanceof Number && valeur2 instanceof Number) {
            return ((Number) valeur1).doubleValue() < ((Number) valeur2).doubleValue();
        }
        if (valeur1 instanceof String && valeur2 instanceof String) {
            return ((String) valeur1).compareTo((String) valeur2) < 0;
        }
        throw new IllegalArgumentException("Type non pris en charge pour la comparaison");
    }

    public boolean superieur(Object valeur1, Object valeur2) {
        if (valeur1 instanceof Number && valeur2 instanceof Number) {
            return ((Number) valeur1).doubleValue() > ((Number) valeur2).doubleValue();
        }
        if (valeur1 instanceof String && valeur2 instanceof String) {
            return ((String) valeur1).compareTo((String) valeur2) > 0;
        }
        throw new IllegalArgumentException("Type non pris en charge pour la comparaison");
    }

    public static int obtenirPositionAttribut(Attribut attribut, Attribut[] attributs) {
        // System.out.println("Attrbiut à chercher : "+attribut.getNom());
        for (int i = 0; i < attributs.length; i++) {
            // System.out.println(" Attribut : "+attributs[i].getNom());
            if (attributs[i].getNom().equals(attribut.getNom())) {

                return i; // Retourner l'indice de l'attribut
            }
        }
    return-1; // Retourner -1 si l'attribut n'est pas trouvé

    }

    // Fonction "et" qui retourne une relation après intersection
    public static Relation et(Condition... conditions) {
        if (conditions.length == 0) {
            return null; // Si aucune condition n'est spécifiée, retourner null (ou une relation vide si
                         // nécessaire
        }

        Relation resultat = conditions[0].evaluer(); // Initialiser avec la première condition

        for (int i = 1; i < conditions.length; i++) {
            Relation conditionResult = conditions[i].evaluer();
            if (conditionResult != null) {
                resultat = Relation.intersection(resultat, conditionResult);
            }
        }

        return resultat;
    }

    // Fonction "ou" qui retourne une relation après union
    public static Relation ou(Condition... conditions) {
        if (conditions.length == 0) {
            return null; // Si aucune condition n'est spécifiée, retourner null (ou une relation vide si
                         // nécessaire
        }

        Relation resultat = null;

        for (Condition condition : conditions) {
            Relation conditionResult = condition.evaluer();
            if (conditionResult != null) {
                if (resultat == null) {
                    resultat = conditionResult; // Initialisation pour la première condition
                } else {
                    resultat = Relation.union(resultat, conditionResult);
                }
            }
        }

        return resultat != null ? resultat : null; // Retourner null si aucune condition n'a produit de résultats
    }
    @Override
public String toString() {
    if (attribut2 != null) {
        // Cas où la condition compare deux attributs
        return attribut.getNom() + " " + operateur + " " + attribut2.getNom();
    } else {
        // Cas où la condition compare un attribut avec une valeur
        return attribut.getNom() + " " + operateur + " " + (valeur != null ? valeur.toString() : "null");
    }
}


}
