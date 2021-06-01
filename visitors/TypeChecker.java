package visitors;


import types.*;
import syntaxtree.*;
import visitor.*;


import java.util.*;


public class TypeChecker extends GJDepthFirst<String, String> {

    private String currentClass;
    private String currentMethod;

    private boolean object;
    private boolean compare;

    private final Map<String, ClassDeclInfo> symbolTable;
    private final Stack<List<String>> callingMethodParameters;

    public TypeChecker(Map<String, ClassDeclInfo> symbolTable) {
        this.compare = false;
        this.object = false;
        this.currentClass = null;
        this.currentMethod = null;
        this.callingMethodParameters = new Stack<>();
        this.symbolTable = symbolTable;
    }

    private String arithmeticExpression(String primaryExpr1, String primaryExpr2) {
        if (!primaryExpr1.equals("int") || !primaryExpr2.equals("int")) {
            if (compare) {
                throw new RuntimeException("Error Type: CompareExpression!\n");
            }
            else {
                throw new RuntimeException("Error Type: ArithmeticExpression!\n");
            }
        }

        return compare ? "boolean" : "int";
    }

    private String variableExists(String currClass, String identifier) {
        if (currClass != null) {
            ClassDeclInfo tempClass = symbolTable.get(currClass);
            Map<String, ClassMember> currClassMembers = tempClass.getClassMembers();
            Map<String, ClassMethod> currClassMethods = tempClass.getClassMethods();

            ClassMethod tempMethod = currClassMethods.get(this.currentMethod);
            if (tempMethod != null) {
                /* check in current method's argument list */
                List<MethodArguments> currMethodArgList = tempMethod.getMethodArgList();
                Map<String, VariableDeclaration> currMethodLocalVariables = tempMethod.getMethodVariables();

                if (currMethodArgList.stream().anyMatch(argObj -> Objects.equals(argObj.getIdentifier(), identifier))) {
                    MethodArguments methodArg = currMethodArgList.stream().filter((obj -> Objects.equals(obj.getIdentifier(), identifier))).findFirst().orElse(null);
                    if (methodArg != null) {
                        return methodArg.getType();
                    }
                    else {
                        throw new RuntimeException("Error Type: Variable does not exists!\n");
                    }
                }

                /* check in current method's local declared variables */
                if (currMethodLocalVariables.containsKey(identifier)) {
                    return currMethodLocalVariables.get(identifier).getType();
                }
            }

            /* check in current class's members */
            if (currClassMembers.containsKey(identifier)) {
                return currClassMembers.get(identifier).getType();
            } else {
                ClassDeclInfo parentClass = tempClass.getExtendsToClass();
                if (parentClass != null) {
                    return variableExists(parentClass.getClassIdentifier(), identifier);
                }
            }
        }
        return null;
    }

    private String overridingMethodExists(String currentClass, String methodIdentifier) {
        if (!symbolTable.containsKey(currentClass)) return null;

        if (currentClass != null) {
            Map<String, ClassMethod> classMethods = symbolTable.get(currentClass).getClassMethods();
            if (classMethods != null) {
                if (classMethods.containsKey(methodIdentifier)) {
                    return classMethods.get(methodIdentifier).getBelongsToClass().getClassIdentifier();
                }
                else {
                    ClassDeclInfo parentClass = symbolTable.get(currentClass).getExtendsToClass();
                    if (parentClass != null) {
                        return overridingMethodExists(parentClass.getClassIdentifier(), methodIdentifier);
                    }
                    else {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private boolean isDerivedClass(String baseClass, String derivedClass) {
        if (!symbolTable.containsKey(baseClass)) return false;
        if (!symbolTable.containsKey(derivedClass)) return false;

        ClassDeclInfo parentClass = symbolTable.get(derivedClass).getExtendsToClass();
        if (parentClass != null) {
            if (!parentClass.getClassIdentifier().equals(baseClass)) {
                return isDerivedClass(baseClass, parentClass.getClassIdentifier());
            }
            else {
                return true;
            }
        }
        return false;
    }

    private void checkTypes(String expressionType, String originalMethodArgType, boolean anInt, boolean aBoolean, boolean isArray) {
        if (!originalMethodArgType.equals(expressionType) && !anInt && !aBoolean && !isArray) {
            if (!isDerivedClass(originalMethodArgType, expressionType)) {
                throw new RuntimeException("Error Type: TypeMissMatch!\n");
            }
        }
        else if (!originalMethodArgType.equals(expressionType)) {
            throw new RuntimeException("Error Type: TypeMissMatch!\n");
        }
    }


    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    @Override
    public String visit(Goal n, String argu) throws Exception {
        n.f0.accept(this, argu);

        if (n.f1.present()) {
            for (int i = 0; i != n.f1.size(); ++i) {
                n.f1.elementAt(i).accept(this, argu);
            }
        }

        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
    @Override
    public String visit(MainClass n, String argu) throws  Exception {
        this.object = true;
        this.currentClass = n.f1.accept(this, argu);
        this.currentMethod = n.f6.tokenImage;


        if (n.f14.present()) {
            for (int i = 0; i != n.f14.size(); ++i) {
                n.f14.elementAt(i).accept(this, this.currentClass);
            }
        }

        if (n.f15.present()) {
            for (int i = 0; i != n.f15.size(); ++i) {
                n.f15.elementAt(i).accept(this, this.currentClass);
            }
        }

        return null;
    }

    /**
    * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
    @Override
    public String visit(TypeDeclaration n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    @Override
    public String visit(ClassDeclaration n, String argu) throws Exception {
        this.object = true;
        this.currentClass = n.f1.accept(this, argu);

        if (n.f3.present()) {
            for (int i = 0; i != n.f3.size(); ++i) {
                n.f3.elementAt(i).accept(this, this.currentClass);
            }
        }

        if (n.f4.present()) {
            for (int i = 0; i != n.f4.size(); ++i) {
                n.f4.elementAt(i).accept(this, this.currentClass);
            }
        }

        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
    @Override
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        this.object = true;
        this.currentClass = n.f1.accept(this, argu);

        if (n.f5.present()) {
            for (int i = 0; i != n.f5.size(); ++i) {
                n.f5.elementAt(i).accept(this, this.currentClass);
            }
        }

        if (n.f6.present()) {
            for (int i = 0; i != n.f6.size(); ++i) {
                n.f6.elementAt(i).accept(this, this.currentClass);
            }
        }

        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    @Override
    public String visit(VarDeclaration n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);

        return null;
    }

    /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
    @Override
    public String visit(MethodDeclaration n, String argu) throws Exception {
        String methodType;
        String methodReturnType;

        methodType = n.f1.accept(this, argu);
        this.object = true;
        this.currentMethod = n.f2.accept(this, argu);

        ClassDeclInfo extendsClass = symbolTable.get(argu).getExtendsToClass();
        if (extendsClass != null) {
            /* name of the class that overriding method came from */
            String classOverridingMethod = overridingMethodExists(extendsClass.getClassIdentifier(), this.currentMethod);
            if (classOverridingMethod != null) {
                ClassMethod overridingMethod = symbolTable.get(classOverridingMethod).getClassMethods().get(this.currentMethod);
                ClassMethod currMethod = symbolTable.get(this.currentClass).getClassMethods().get(this.currentMethod);

                List<MethodArguments> overridingMethodArgList = overridingMethod.getMethodArgList();
                List<MethodArguments> currentMethodArgList = currMethod.getMethodArgList();

                if (currentMethodArgList.size() != overridingMethodArgList.size()) {
                    throw new RuntimeException("Error Type: Overriding Method does not have the same amount of parameters!\n");
                }

                if (!overridingMethod.getReturnType().equals(currMethod.getReturnType())) {
                    throw new RuntimeException("Error Type: Overriding Method does not have the same return type!\n");
                }

                Iterator<MethodArguments> currentIterator = currentMethodArgList.iterator();
                Iterator<MethodArguments> overridingIterator = overridingMethodArgList.iterator();

                while (currentIterator.hasNext() && overridingIterator.hasNext()) {
                    MethodArguments currArg = currentIterator.next();
                    MethodArguments overridingArg = overridingIterator.next();

                    if (!currArg.getType().equals(overridingArg.getType())) {
                        throw new RuntimeException("Error Type: Method Parameters are not the same!\n");
                    }
                }
            }
        }

        if (n.f4.present()) {
            n.f4.accept(this, argu);
        }

        if (n.f7.present()) {
            for (int i = 0; i != n.f7.size(); ++i) {
                n.f7.elementAt(i).accept(this, argu);
            }
        }

        if (n.f8.present()) {
            for (int i = 0; i != n.f8.size(); ++i) {
                n.f8.elementAt(i).accept(this, argu);
            }
        }

        methodReturnType = n.f10.accept(this, argu);
        checkTypes(methodReturnType, methodType, false, false, false);

        return null;
    }

    /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
    @Override
    public String visit(FormalParameterList n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);

        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    @Override
    public String visit(FormalParameter n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);

        return null;
    }

    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    @Override
    public String visit(Type n, String argu) throws Exception {
        return n.f0.accept(this, "type");
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    @Override
    public String visit(ArrayType n, String argu) {
        return "int[]";
    }

    /**
    * f0 -> "boolean"
    */
    @Override
    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

    /**
    * f0 -> "int"
    */
    @Override
    public String visit(IntegerType n, String argu) {
        return "int";
    }

    /**
     * f0 -> Block()
     *       | AssignmentStatement()
     *       | ArrayAssignmentStatement()
     *       | IfStatement()
     *       | WhileStatement()
     *       | PrintStatement()
     */
    public String visit(Statement n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
    @Override
    public String visit(Block n, String argu) throws Exception {
        if (n.f1.present()) {
            for (int i = 0; i != n.f1.size(); ++i) {
                n.f1.elementAt(i).accept(this, argu);
            }
        }

        return null;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    @Override
    public String visit(AssignmentStatement n, String argu) throws Exception {
        String identifierType;
        String expressionType;

        identifierType = n.f0.accept(this, argu);
        expressionType = n.f2.accept(this, argu);

        checkTypes(expressionType, identifierType, identifierType.equals("int"), identifierType.equals("boolean"), identifierType.equals("int[]"));

        return null;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    @Override
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        String identifierType;
        String expressionInsideBrackets;
        String expression;

        identifierType = n.f0.accept(this, argu);
        expressionInsideBrackets = n.f2.accept(this, argu);
        expression = n.f5.accept(this, argu);

        if (!identifierType.equals("int[]")) {
            throw new RuntimeException("Error Type: ArrayAssignmentStatement!\n");
        }

        if (!expressionInsideBrackets.equals("int")) {
            throw new RuntimeException("Error Type: ArrayAssignmentStatement!\n");
        }

        if (!expression.equals("int")) {
            throw new RuntimeException("Error Type: ArrayAssignmentStatement!\n");
        }

        return null;
    }

    /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    @Override
    public String visit(IfStatement n, String argu) throws Exception {
        String ifStatement;

        ifStatement = n.f2.accept(this, argu);
        if (!ifStatement.equals("boolean")) {
            throw new RuntimeException("Error Type: IfStatement!\n");
        }
        n.f4.accept(this, argu);
        n.f6.accept(this, argu);

        return null;
    }

    /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    @Override
    public String visit(WhileStatement n, String argu) throws Exception {
        String whileStatement;

        whileStatement = n.f2.accept(this, argu);
        if (!whileStatement.equals("boolean")) {
            throw new RuntimeException("Error Type: WhileStatement!\n");
        }
        n.f4.accept(this, argu);

        return null;
    }

    /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    @Override
    public String visit(PrintStatement n, String argu) throws Exception {
        String printStatement;

        printStatement = n.f2.accept(this, argu);
        if (!printStatement.equals("int")) {
            throw new RuntimeException("Error Type: PrintStatement!\n");
        }

        return null;
    }

    /**
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | PrimaryExpression()
     */
    public String visit(Expression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "&&"
     * f2 -> PrimaryExpression()
     */
    public String visit(AndExpression n, String argu) throws Exception {
        String pe1;
        String pe2;

        pe1 = n.f0.accept(this, argu);
        pe2 = n.f2.accept(this, argu);

        if (!pe1.equals("boolean") || !pe2.equals("boolean")) {
            throw new RuntimeException("Error Type: AndExpression!\n");
        }

        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, String argu) throws Exception {
        String pe1;
        String pe2;

        pe1 = n.f0.accept(this, argu);
        pe2 = n.f2.accept(this, argu);
        this.compare = true;

        return arithmeticExpression(pe1, pe2);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, String argu) throws Exception {
        String pe1;
        String pe2;

        pe1 = n.f0.accept(this, argu);
        pe2 = n.f2.accept(this, argu);
        this.compare = false;

        return arithmeticExpression(pe1, pe2);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, String argu) throws Exception {
        String pe1;
        String pe2;

        pe1 = n.f0.accept(this, argu);
        pe2 = n.f2.accept(this, argu);
        this.compare = false;

        return arithmeticExpression(pe1, pe2);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(TimesExpression n, String argu) throws Exception {
        String pe1;
        String pe2;

        pe1 = n.f0.accept(this, argu);
        pe2 = n.f2.accept(this, argu);
        this.compare = false;

        return arithmeticExpression(pe1, pe2);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    @Override
    public String visit(ArrayLookup n, String argu) throws Exception {
        String pe1;
        String pe2;

        pe1 = n.f0.accept(this, argu);
        pe2 = n.f2.accept(this, argu);

        if (!pe1.equals("int[]") || !pe2.equals("int")) {
            throw new RuntimeException("Error Type: ArrayLookup!\n");
        }

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, String argu) throws Exception {
        String primaryExpression;

        primaryExpression = n.f0.accept(this, argu);

        if (!primaryExpression.equals("int[]")) {
            throw new RuntimeException("Error Type: ArrayLength!\n");
        }

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    @Override
    public String visit(MessageSend n, String argu) throws Exception {
        String classIdentifier;
        String methodIdentifier;

        classIdentifier = n.f0.accept(this, argu);
        this.object = true;
        methodIdentifier = n.f2.accept(this, argu);

        List<String> callMethodParams = new LinkedList<>();
        this.callingMethodParameters.push(callMethodParams);

        String classThatMethodExists = this.overridingMethodExists(classIdentifier, methodIdentifier);
        if (classThatMethodExists == null) {
            throw new RuntimeException("Error Type: MessageSend!\n");
        }

        if (n.f4.present()) {
            n.f4.accept(this, classIdentifier);
        }

        List<String> callParams = this.callingMethodParameters.pop();
        List<MethodArguments> methodParams = symbolTable.get(classThatMethodExists).getClassMethods().get(methodIdentifier).getMethodArgList();

        if (callParams.size() != methodParams.size()) {
            throw new RuntimeException("Error Type: MessageSend!\n");
        }

        for (int i = 0; i != methodParams.size(); ++i) {
            checkTypes( callParams.get(i), methodParams.get(i).getType(), methodParams.get(i).getType().equals("int"),
                        methodParams.get(i).getType().equals("boolean"), methodParams.get(i).getType().equals("int[]"));
        }

        return symbolTable.get(classThatMethodExists).getClassMethods().get(methodIdentifier).getReturnType();
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    @Override
    public String visit(ExpressionList n, String argu) throws Exception {
        String expressionType = n.f0.accept(this, argu);

        List<String> params = this.callingMethodParameters.pop();
        params.add(expressionType);
        this.callingMethodParameters.push(params);

        n.f1.accept(this, argu);

        return null;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    @Override
    public String visit(ExpressionTail n, String argu) throws Exception {
        if (n.f0.present()) {
            for (int i = 0; i != n.f0.size(); ++i) {
                n.f0.elementAt(i).accept(this, argu);
            }
        }

        return null;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    @Override
    public String visit(ExpressionTerm n, String argu) throws Exception {
        String expressionType = n.f1.accept(this, argu);

        List<String> params = this.callingMethodParameters.pop();
        params.add(expressionType);
        this.callingMethodParameters.push(params);

        return null;
    }

    /**
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | NotExpression()
     *       | BracketExpression()
     */
    @Override
    public String visit(PrimaryExpression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /*
    * f0 -> <INTEGER_LITERAL>
    */
    @Override
    public String visit(IntegerLiteral n, String argu) throws Exception {
        return "int";
    }

    /**
    * f0 -> "true"
    */
    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
        return "boolean";
    }

    /**
    * f0 -> "false"
    */
    @Override
    public String visit(FalseLiteral n, String argu) throws Exception {
        return "boolean";
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    @Override
    public String visit(Identifier n, String argu) throws Exception {
        String identifier;
        String type;

        if (argu != null && argu.equals("type")) {
            String classTypeIdentifier = n.f0.accept(this, argu);
            if (!symbolTable.containsKey(classTypeIdentifier)) {
                throw new RuntimeException("Error Type: Undefined Object Type!\n");
            }
            return symbolTable.get(classTypeIdentifier).getClassIdentifier();
        }
        if (this.object) {
            this.object = false;
            return n.f0.tokenImage;
        }
        else {
            identifier = n.f0.accept(this, argu);
            if ((type = variableExists(this.currentClass, identifier)) == null) {
                throw new RuntimeException("Error Type: Variable does not exists!\n");
            }
            return type;
        }
    }

    /**
    * f0 -> "this"
    */
    @Override
    public String visit(ThisExpression n, String argu) throws Exception {
        return symbolTable.get(this.currentClass).getClassIdentifier();
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    @Override
    public String visit(ArrayAllocationExpression n, String argu) throws Exception {
        String arrayAllocationExpression;

        arrayAllocationExpression = n.f3.accept(this, argu);
        if (!arrayAllocationExpression.equals("int")) {
            throw new RuntimeException("Error Type: ArrayAllocationExpression!\n");
        }

        return "int[]";
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    @Override
    public String visit(AllocationExpression n, String argu) throws Exception {
        this.object = true;
        return n.f1.accept(this, argu);
    }

    /**
    * f0 -> "!"
    * f1 -> PrimaryExpression()
    */
    @Override
    public String visit(NotExpression n, String argu) throws  Exception {
        String clause;

        clause = n.f1.accept(this, argu);
        if (!clause.equals("boolean")) {
            throw new RuntimeException("Error Type: NotExpression\n");
        }

        return clause;
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    @Override
    public String visit(BracketExpression n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    @Override
    public String visit(NodeToken n, String argu) { return n.tokenImage; }
}
