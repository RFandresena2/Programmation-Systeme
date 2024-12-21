package rlt;
public class Ligne {
    private Object[] elements;

    // Constructeur
    public Ligne(Object[] elements) {
        this.elements = elements;
    }

    // Getter pour obtenir les éléments de la ligne
    public Object[] getElements() {
        return elements;
    }

    // Setter pour modifier les éléments de la ligne
    public void setElements(Object[] elements) {
        this.elements = elements;
    }

    // Méthode pour obtenir un élément à un index spécifique
    public Object getElement(int index) {
        if (index >= 0 && index < elements.length) {
            return elements[index];
        }
        throw new IndexOutOfBoundsException("Index invalide : " + index);
    }

    // Méthode pour définir un élément à un index spécifique
    public void setElement(int index, Object value) {
        if (index >= 0 && index < elements.length) {
            elements[index] = value;
        } else {
            throw new IndexOutOfBoundsException("Index invalide : " + index);
        }
    }

    // Méthode pour afficher les informations de la ligne
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Ligne{ ");
        for (Object element : elements) {
            sb.append(element).append(" ");
        }
        sb.append("}");
        return sb.toString();
    }
}
