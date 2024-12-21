package domaine;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DomaineMixte extends Domaine {
    private List<Class<?>> types; // Liste de classes représentant le domaine infini
    private Set<Object> elements; // Ensemble représentant le domaine fini

    public DomaineMixte(List<Class<?>> types, Set<Object> elements) {
        this.types = types;
        this.elements = elements;
    }

    @Override
    public boolean appartient(Object element) {
        if(element == null)
        {
            return true;
        }
        // Vérifie si l'élément appartient au domaine fini ou correspond à un des types
        boolean appartientAuType = types.stream().anyMatch(type -> type.isInstance(element));
        return elements.contains(element) || appartientAuType;
    }

    @Override
    public String afficherDomaine() {
        // Combine les informations du domaine fini et des domaines infinis
        StringBuilder typesStr = new StringBuilder("[");
        for (Class<?> type : types) {
            typesStr.append(type.getName()).append(", ");
        }
        if (!types.isEmpty()) {
            typesStr.setLength(typesStr.length() - 2); // Supprime la dernière virgule
        }
        typesStr.append("]");

        return "DomaineMixte : [Classes = " + typesStr.toString() + ", Valeurs Finies = " + elements.toString() + "]";
    }

    // Getters et Setters
    public List<Class<?>> getTypes() {
        return types;
    }

    public void setTypes(List<Class<?>> types) {
        this.types = types;
    }

    public Set<Object> getElements() {
        return elements;
    }

    public void setElements(Set<Object> elements) {
        this.elements = elements;
    }

    @Override
    public String toString() {
        // Génère une chaîne : new DomaineMixte(Set.of(1, 2, 3),
        // List.of(java.lang.Integer.class))
        StringBuilder builder = new StringBuilder("new DomaineMixte(Set.of(");
        builder.append(elements.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ")));
        builder.append("), List.of(");
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
