package visitors;

import syntaxtree.*;
import visitor.*;

import types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DeclCollector extends GJDepthFirst<String, String> {

    private boolean insideMethod;

    private final Map<String, ClassDeclInfo> classSymbolTable;
    private final Map<String, ClassMethod> methodSymbolTable;
    private final Map<String, List<ClassMethod>> methodsInserted;


    public DeclCollector() {
        this.insideMethod = false;
        this.methodsInserted = new HashMap<>();
        this.methodSymbolTable = new HashMap<>();
        this.classSymbolTable = new HashMap<>();
    }

    public Map<String, ClassDeclInfo> getClassSymbolTable() {
        return this.classSymbolTable;
    }

    public void printOffsets() {
        for (ClassDeclInfo classInfo : classSymbolTable.values()) {
            Map<String, ClassMember> classMembers = classInfo.getClassMembers();
            if (classMembers != null) {
                for (ClassMember classMember : classMembers.values()) {
                    System.out.println(classMember.getBelongsToClass().getClassIdentifier() + "." + classMember.getIdentifier() + " : " + classMember.getClassMemberOffset());
                }
            }
            Map<String, ClassMethod> classMethods = classInfo.getClassMethods();
            if (classMethods != null) {
                for (ClassMethod classMethod : classMethods.values()) {
                    if (classMethod.getIdentifier().equals("main")) continue;
                    System.out.println(classMethod.getBelongsToClass().getClassIdentifier() + "." + classMethod.getIdentifier() + " : " + classMethod.getMethodOffset());
                }
            }
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
    public String visit(MainClass n, String argu) throws Exception {
        String classIdentifier;
        String argIdentifier;
        String methodReturnType;

        classIdentifier = n.f1.accept(this, argu);
        methodReturnType = n.f5.tokenImage;
        argIdentifier = n.f11.accept(this, argu);

        /* Main Class should not have an "extends Class" */
        ClassDeclInfo classDeclInfo = new ClassDeclInfo(classIdentifier);
        classSymbolTable.put(classIdentifier, classDeclInfo);

        this.methodsInserted.put(classIdentifier, new ArrayList<>());

        ClassMethod classMethod = new ClassMethod(methodReturnType, "main", 0, classDeclInfo);
        this.methodsInserted.get(classIdentifier).add(classMethod);

        if (classMethod.addMethodArgument("String[]", argIdentifier)) {
            throw new RuntimeException("Error Type: Method argument already exists!\n");
        }
        methodSymbolTable.put("main", classMethod);

        if (classDeclInfo.addMethodToClass("main", classMethod)) {
            throw new RuntimeException("Error Type: Method already exists in this class!\n");
        }

        if (n.f14.present()) {
            insideMethod = true;
            for (int i = 0; i != n.f14.size(); ++i) {
                n.f14.elementAt(i).accept(this, "main");
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
         ClassDeclInfo classDeclInfo;
         String classIdentifier;

         classIdentifier = n.f1.accept(this, argu);

         if (!this.classSymbolTable.containsKey(classIdentifier)) {
             classDeclInfo = new ClassDeclInfo(classIdentifier);
             this.classSymbolTable.put(classIdentifier, classDeclInfo);
             this.methodsInserted.put(classIdentifier, new ArrayList<>());
         }
         else {
             throw new RuntimeException("Error Type: Class " + classIdentifier + "already exists!\n");
         }

         this.insideMethod = false;
         if (n.f3.present()) {
             for (int i = 0; i != n.f3.size(); ++i) {
                 n.f3.elementAt(i).accept(this, classIdentifier);
             }
         }

         if (n.f4.present()) {
             for (int i = 0; i != n.f4.size(); ++i) {
                 n.f4.elementAt(i).accept(this, classIdentifier);
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
         int memberOffset;
         int methodOffset;
         ClassDeclInfo classDeclInfo;
         String classIdentifier;
         String classExtendsIdentifier;

         classIdentifier = n.f1.accept(this, argu);
         classExtendsIdentifier = n.f3.accept(this, argu);

         ClassDeclInfo classInfo = classSymbolTable.get(classExtendsIdentifier);
         if (classInfo == null) {
             throw new RuntimeException("Error Type: ClassExtendsDeclaration!\n");
         }
         memberOffset = classInfo.getLastMemberInsertedOffset();
         methodOffset = classInfo.getLastMethodInsertedOffset();

         if (!this.classSymbolTable.containsKey(classIdentifier) && this.classSymbolTable.containsKey(classExtendsIdentifier)) {
             classDeclInfo = new ClassDeclInfo(classIdentifier, memberOffset, methodOffset, this.classSymbolTable.get(classExtendsIdentifier));
             this.classSymbolTable.put(classIdentifier, classDeclInfo);
             this.methodsInserted.put(classIdentifier, new ArrayList<>());
         }
         else {
             throw new RuntimeException("Error Type: Class " + classIdentifier + " already exists!\n");
         }

         this.insideMethod = false;
         if (n.f5.present()) {
             for (int i = 0; i != n.f5.size(); ++i) {
                 n.f5.elementAt(i).accept(this, classIdentifier);
             }
         }

         if (n.f6.present()) {
             for (int i = 0; i != n.f6.size(); ++i) {
                 n.f6.elementAt(i).accept(this, classIdentifier);
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
         int previousOffset;
         String varType;
         String varIdentifier;
         ClassMethod inMethod;
         ClassDeclInfo inClass;

         varType = n.f0.accept(this, argu);
         varIdentifier = n.f1.accept(this, argu);

         if (this.insideMethod) {
             inMethod = this.methodSymbolTable.get(argu);
             if (!inMethod.addMethodVariable(varType, varIdentifier)) {
                 throw new RuntimeException("Error Type: Local variable in method " + argu + " already exists!\n");
             }
         }
         else {
             inClass = this.classSymbolTable.get(argu);
             if (inClass == null) {
                 throw new RuntimeException("Error Type: Variable Declaration in Class!\n");
             }

             previousOffset = inClass.getLastMemberInsertedOffset();
             if (varType.equals("int")) {
                 inClass.setLastMemberInsertedOffset(previousOffset + 4);
             }
             else if (varType.equals("boolean")) {
                 inClass.setLastMemberInsertedOffset(previousOffset + 1);
             }
             else {
                 inClass.setLastMemberInsertedOffset(previousOffset + 8);
             }

             if (!inClass.addMemberToClass(varIdentifier, new ClassMember(varType, varIdentifier, previousOffset, inClass))) {
                 throw new RuntimeException("Error Type: Member [" + varIdentifier + "] in this class [" + inClass.getClassIdentifier() + "] already exists!\n");
             }
         }

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
         int methodOffset = 0;
         boolean flag = false;
         String returnType;
         String methodIdentifier;
         ClassMethod classMethod;

         returnType = n.f1.accept(this, argu);
         methodIdentifier = n.f2.accept(this, argu);

         if (classSymbolTable.containsKey(methodIdentifier)) {
             throw new RuntimeException("Error Type: Class already exists with that name!\n");
         }

         ClassDeclInfo currClass = classSymbolTable.get(argu);
         ClassDeclInfo parentClass = currClass.getExtendsToClass();

         if (parentClass != null) {
             methodOffset = parentClass.getParentsMethodOffset(methodIdentifier);
             if (methodOffset != -1) {
                 flag = true;
             }
         }

         if (!flag) {
             methodOffset = currClass.getLastMethodInsertedOffset();
             currClass.setLastMethodInsertedOffset(methodOffset + 8);
         }

         classMethod = new ClassMethod(returnType, methodIdentifier, methodOffset, classSymbolTable.get(argu));
         if (this.classSymbolTable.get(argu).addMethodToClass(methodIdentifier, classMethod)) {
             throw new RuntimeException("Error Type: Function overloading!\n");
         }

         this.methodSymbolTable.put(methodIdentifier, classMethod);

         if (n.f4.present()) {
             n.f4.accept(this, methodIdentifier);
         }

         if (n.f7.present()) {
             this.insideMethod = true;
             for (int i = 0; i != n.f7.size(); ++i) {
                 n.f7.elementAt(i).accept(this, methodIdentifier);
             }
         }

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
         ClassMethod classMethod;
         String argType;
         String argIdentifier;

         argType = n.f0.accept(this, argu);
         argIdentifier = n.f1.accept(this, argu);

         classMethod = methodSymbolTable.get(argu);
         if (classMethod.addMethodArgument(argType, argIdentifier)) {
             throw new RuntimeException("Error Type: Argument in method " + argu + " already declared!\n");
         }

         return null;
     }

     /**
     * f0 -> ( FormalParameterTerm() )*
     */
     @Override
     public String visit(FormalParameterTail n, String argu) throws Exception {
         if (n.f0.present()) {
             for (int i = 0; i != n.f0.size(); ++i) {
                 n.f0.elementAt(i).accept(this, argu);
             }
         }
         return null;
     }

     /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
     @Override
     public String visit(FormalParameterTerm n, String argu) throws Exception {
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
         return n.f0.accept(this, argu);
     }

     /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
     @Override
     public String visit(ArrayType n, String argu) {
         return n.f0.tokenImage + n.f1.tokenImage + n.f2.tokenImage;
     }

     /**
     * f0 -> "boolean"
     */
     @Override
     public String visit(BooleanType n, String argu) {
         return n.f0.tokenImage;
     }

     /**
     * f0 -> "int"
     */
     @Override
     public String visit(IntegerType n, String argu) {
         return n.f0.tokenImage;
     }

     /**
     * f0 -> <IDENTIFIER>
     */
     @Override
     public String visit(Identifier n, String argu) {
         return n.f0.tokenImage;
     }
}
