package types;

import java.util.*;


public class ClassMethod {

    private final String returnType;
    private final String identifier;
    private final int methodOffset;

    private final ClassDeclInfo belongsToClass;
    
    private final List<MethodArguments> methodArgList;
    private final Map<String, VariableDeclaration> methodVariables;


    public ClassMethod(String retType, String id, int methodOffset, ClassDeclInfo belongsToClass) {
        this.returnType = retType;
        this.identifier = id;
        this.methodOffset = methodOffset;
        this.belongsToClass = belongsToClass;
        this.methodArgList = new ArrayList<>();
        this.methodVariables = new HashMap<>();
    }

    public String getReturnType() {
        return this.returnType;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public int getMethodOffset() {
        return this.methodOffset;
    }

    public ClassDeclInfo getBelongsToClass() {
        return this.belongsToClass;
    }

    public List<MethodArguments> getMethodArgList() {
        return this.methodArgList;
    }

    public Map<String, VariableDeclaration> getMethodVariables() {
        return this.methodVariables;
    }

    public boolean addMethodArgument(String type, String id) {
        long count;

        count = methodArgList.stream().filter(argObj -> Objects.equals(argObj.getIdentifier(), id)).count();
        if (count == 0) {
            methodArgList.add(new MethodArguments(type, id));
            return false;
        }

        return true;
    }

    public boolean addMethodVariable(String type, String varId) {

        if (methodArgList.stream().anyMatch(argObj -> Objects.equals(argObj.getIdentifier(), varId))) {
            return false;
        }
        if (!methodVariables.containsKey(varId)) {
            methodVariables.put(varId, new VariableDeclaration(type, varId));
            return true;
        }

        return false;
    }

}
