package domaine;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Iterator;

public class DomaineFini extends Domaine {
    private Set<Object> elements;

    public DomaineFini(Set<Object> elements) {
        this.elements = elements;
    }

    @Override
    public boolean appartient(Object element) {
        if(element == null)
        {
            return true;
        }
        return elements.contains(element);
    }

    @Override
    public String afficherDomaine() {
        return "DomaineFini : " + elements.toString(); // Affiche les éléments du domaine
    }

    public Set<Object> getElements() {
        return elements;
    }

    public void setElements(Set<Object> elements) {
        this.elements = elements;
    }
    @Override
public String toString() {
    StringBuilder sb = new StringBuilder("new DomaineFini(Set.of(");
    Iterator<Object> it = elements.iterator();
    while (it.hasNext()) {
        Object valeur = it.next();
        if (valeur instanceof String) {
            sb.append("\"").append(valeur).append("\""); // Ajouter des guillemets pour les chaînes
        } else {
            sb.append(valeur); // Conserver le format pour les autres types
        }
        if (it.hasNext()) {
            sb.append(", ");
        }
    }
    sb.append("))");
    return sb.toString();
}


}

