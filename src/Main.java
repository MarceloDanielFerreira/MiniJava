import java.io.InputStreamReader;

import ast.ClassDecl;
import ast.ClassDeclExtends;
import ast.ClassDeclSimple;
import ast.Goal;
import ast.visitor.MiniJPrintVisitor;
import ast.visitor.OptimizationVisitor;
import ast.visitor.ASTPrinterVisitor;
import ast.visitor.SemanticAnalyzerVisitor;
import ast.visitor.SemanticError;
import ast.visitor.Visitor;
import java_cup.runtime.Symbol;
import java.util.List;
import ast.visitor.JCodeGenVisitor;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        InputStreamReader isr = new InputStreamReader(System.in);
        Scanner s = new Scanner(isr);
        parser p = new parser(s);
        try {
            Symbol root = p.parse();
            Goal g = (Goal) root.value;
            
            System.out.println("======================");
            System.out.println("  MINI-J CODE OUTPUT  ");
            System.out.println("======================");
            Visitor mj = new MiniJPrintVisitor();
            mj.visit(g);
            
            System.out.println("\n\n======================");
            System.out.println("    AST TREE OUTPUT   ");
            System.out.println("======================");
            Visitor ast = new ASTPrinterVisitor();
            ast.visit(g);
            
            // Análisis semántico
            System.out.println("\n======================");
            System.out.println("  ANALISIS SEMANTICO   ");
            System.out.println("======================");
            SemanticAnalyzerVisitor semantic = new SemanticAnalyzerVisitor();
            List<SemanticError> errors = semantic.analyze(g);

            // Optimización
            System.out.println("\n======================");
            System.out.println("  OPTIMIZACION  ");
            System.out.println("======================");
            OptimizationVisitor optimizer = new OptimizationVisitor();
            optimizer.visit(g);
            System.out.println("Optimización completada sin advertencias.");

            // Solo generar código si no hay errores semánticos
            if (errors.isEmpty()) {
                System.out.println("\n======================");
                System.out.println("  GENERACION DE CODIGO  ");
                System.out.println("======================");

                // Generación de código Jasmin
                JCodeGenVisitor codegen = new JCodeGenVisitor();
                codegen.visit(g);

                // Mostrar el contenido de cada archivo .j generado en consola
                // Obtener nombres de clases desde el AST
                List<String> classNames = new ArrayList<>();
                // MainClass
                classNames.add(g.m.i1.s);
                // Otras clases
                for (int i = 0; i < g.cl.size(); i++) {
                    ClassDecl c = g.cl.get(i);
                    if (c instanceof ClassDeclSimple) {
                        classNames.add(((ClassDeclSimple) c).i.s);
                    } else if (c instanceof ClassDeclExtends) {
                        classNames.add(((ClassDeclExtends) c).i.s);
                    }
                }
                for (String className : classNames) {
                    String fileName = className + ".j";
                    System.out.println("\n--- " + fileName + " ---");
                    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            System.out.println(line);
                        }
                    } catch (IOException e) {
                        System.out.println("No se pudo leer el archivo " + fileName);
                    }
                }
                System.out.println("\nArchivos .j generados en el directorio actual.");
            } else {
                System.out.println("\n======================");
                System.out.println("  NO SE GENERA CODIGO  ");
                System.out.println("======================");
                System.out.println("Como se detectaron errores semanticos, no se procederá a la generacion de codigo.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void report_error(String message, Object info) {
        System.err.print(message);
        System.err.flush();
        if (info instanceof Symbol)
            if (((Symbol) info).left != -1)
                System.err.println(" at line " + ((Symbol) info).left + " of input");
            else
                System.err.println("");
        else
            System.err.println("");
    }
}