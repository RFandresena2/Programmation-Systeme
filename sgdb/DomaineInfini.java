package domaine;
import java.util.List;

public class DomaineInfini extends Domaine {
    private List<Class<?>> types;

    public DomaineInfini(List<Class<?>> types) {
        this.types = types;
    }

    @Override
    public boolean appartient(Object element) {
        if(element == null)
        {
            return true;
        }
        // Vérifie si l'élément appartient à l'un des types dans la liste
        for (Class<?> type : types) {
            if (type.isInstance(element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String afficherDomaine() {
        // Affiche les noms de toutes les classes dans la liste
        StringBuilder sb = new StringBuilder("DomaineInfini : ");
        for (Class<?> type : types) {
            sb.append(type.getName()).append(" ");
        }
        return sb.toString().trim();
    }

    public List<Class<?>> getTypes() {
        return types;
    }

    public void setTypes(List<Class<?>> types) {
        this.types = types;
    }
    @Override
    public String toString() {
        // Génère une chaîne : new DomaineInfini(List.of(java.lang.Integer.class, java.lang.String.class))
        StringBuilder builder = new StringBuilder("new DomaineInfini(List.of(");
        for (int i = 0; i < types.size(); i++) {
            builder.append(types.get(i).getName()).append(".class");
            if (i < types.size() - 1) {
                builder.append(", ");
            }
        }
        builder.append("))");
        return builder.toString();
    }
    

    
}
