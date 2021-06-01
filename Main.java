import syntaxtree.*;

import visitors.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length < 1){
            System.err.println("Usage: java Main <inputFile>");
            System.exit(1);
        }

        FileInputStream fis = null;
        for (int i = 0; i != args.length; ++i) {
            try {
                fis = new FileInputStream(args[i]);

                MiniJavaParser parser = new MiniJavaParser(fis);
                Goal root = parser.Goal();

                System.out.println("Semantic Analysis on " + args[i]);

                DeclCollector declCollector = new DeclCollector();
                root.accept(declCollector, null);

                TypeChecker typeChecker = new TypeChecker(declCollector.getClassSymbolTable());
                root.accept(typeChecker, null);

                declCollector.printOffsets();
                System.out.println("Semantic Analysis finished successfully!\n");
            }
            catch(ParseException | RuntimeException | FileNotFoundException ex) {
                System.out.println(ex.getMessage());
            }
            finally {
                try {
                    if(fis != null) fis.close();
                }
                catch(IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
