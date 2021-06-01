package types;

import java.util.Objects;


public class VariableDeclaration {
    
    private final String type;
    private final String identifier;

    public VariableDeclaration(String type, String id) {
        this.type = type;
        this.identifier = id;
    }

    public String getType() {
        return this.type;
    }
    
    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        VariableDeclaration var = (VariableDeclaration) obj;
        return Objects.equals(identifier, var.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
