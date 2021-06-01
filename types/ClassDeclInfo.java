package types;

import java.util.Map;
import java.util.HashMap;


public class ClassDeclInfo {

    private final String classIdentifier;
    private int lastMemberInsertedOffset;
    private int lastMethodInsertedOffset;
    
    private final ClassDeclInfo extendsToClass;
    
    private final Map<String, ClassMember> classMembers;
    private final Map<String, ClassMethod> classMethods;


    public ClassDeclInfo(String identifier) {
        this.lastMethodInsertedOffset = 0;
        this.lastMemberInsertedOffset = 0;
        this.classIdentifier = identifier;
        this.extendsToClass = null;
        this.classMembers = new HashMap<>();
        this.classMethods = new HashMap<>();
    }

    public ClassDeclInfo(String identifier, int classMemberOffset, int classMethodOffset, ClassDeclInfo extendsToClass) {
        this.classIdentifier = identifier;
        this.extendsToClass = extendsToClass;
        this.lastMemberInsertedOffset = classMemberOffset;
        this.lastMethodInsertedOffset = classMethodOffset;
        this.classMembers = new HashMap<>();
        this.classMethods = new HashMap<>();
    }
    
    public String getClassIdentifier() {
        return this.classIdentifier;
    }

    public ClassDeclInfo getExtendsToClass() {
        return this.extendsToClass;
    }

    public int getLastMemberInsertedOffset() {
        return this.lastMemberInsertedOffset;
    }

    public void setLastMemberInsertedOffset(int offset) {
        this.lastMemberInsertedOffset = offset;
    }

    public int getLastMethodInsertedOffset() {
        return this.lastMethodInsertedOffset;
    }

    public void setLastMethodInsertedOffset(int offset) {
        this.lastMethodInsertedOffset = offset;
    }

    public Map<String, ClassMember> getClassMembers() {
        return this.classMembers;
    }

    public Map<String, ClassMethod> getClassMethods() {
        return this.classMethods;
    }

    public int getParentsMethodOffset(String methodIdentifier) {
        if (this.classMethods.containsKey(methodIdentifier)) {
            return this.classMethods.get(methodIdentifier).getMethodOffset();
        }
        else if (this.extendsToClass != null) {
            return this.extendsToClass.getParentsMethodOffset(methodIdentifier);
        }
        else {
            return -1;
        }
    }

    public boolean addMemberToClass(String memberIdentifier, ClassMember classMember) {

        if (!classMembers.containsKey(memberIdentifier)) {
            classMembers.put(memberIdentifier, classMember);
            return true;
        }

        return false;
    }

    public boolean addMethodToClass(String methodIdentifier, ClassMethod classMethod) {

        if (!classMethods.containsKey(methodIdentifier)) {
            classMethods.put(methodIdentifier, classMethod);
            return false;
        }

        return true;
    }
}

