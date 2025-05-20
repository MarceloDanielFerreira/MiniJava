import java.io.InputStreamReader;
import ast.Goal;
import ast.visitor.MiniJPrintVisitor;
import ast.visitor.ASTPrinterVisitor;
import ast.visitor.SemanticAnalyzerVisitor;
import ast.visitor.SemanticError;
import ast.visitor.Visitor;
import java_cup.runtime.Symbol;

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
            
            // Run semantic analysis last
            System.out.println("\n\n======================");
            System.out.println("  SEMANTIC ANALYSIS   ");
            System.out.println("======================");
            SemanticAnalyzerVisitor semantic = new SemanticAnalyzerVisitor();
            semantic.visit(g);
            
            // Print semantic errors if any
            if (!semantic.getErrors().isEmpty()) {
                System.out.println("\nSemantic errors found:");
                for (SemanticError error : semantic.getErrors()) {
                    System.out.println(error.getMessage());
                }
            } else {
                System.out.println("\nNo semantic errors found.");
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