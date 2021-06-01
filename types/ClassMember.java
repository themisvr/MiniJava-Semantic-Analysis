package types;


public class ClassMember extends VariableDeclaration {
    
    private final ClassDeclInfo belongsToClass;
    private final int classMemberOffset;

    public ClassMember(String type, String identifier, int classMemberOffset, ClassDeclInfo belongsToClass) {
        super(type, identifier);
        this.classMemberOffset = classMemberOffset;
        this.belongsToClass = belongsToClass;
    }

    public ClassDeclInfo getBelongsToClass() {
        return this.belongsToClass;
    }

    public int getClassMemberOffset() {
        return this.classMemberOffset;
    }
}
