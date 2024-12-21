package domaine;
public abstract class Domaine {
    // Fonction abstraite : vérifie si un élément appartient au domaine
    public abstract boolean appartient(Object element);

    // Fonction abstraite : affiche le domaine
    public abstract String afficherDomaine();
}

