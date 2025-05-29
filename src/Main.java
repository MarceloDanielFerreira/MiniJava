import java.io.InputStreamReader;
import ast.Goal;
import ast.visitor.MiniJPrintVisitor;
import ast.visitor.OptimizationVisitor;
import ast.visitor.ASTPrinterVisitor;
import ast.visitor.SemanticAnalyzerVisitor;
import ast.visitor.SemanticError;
import ast.visitor.Visitor;
import java_cup.runtime.Symbol;
import java.util.List;

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
            System.out.println("  SEMANTIC ANALYSIS   ");
            System.out.println("======================");
            SemanticAnalyzerVisitor semantic = new SemanticAnalyzerVisitor();
            List<SemanticError> errors = semantic.analyze(g);

            // Optimización
            System.out.println("\n======================");
            System.out.println("  OPTIMIZATION PHASE   ");
            System.out.println("======================");
            OptimizationVisitor optimizer = new OptimizationVisitor();
            optimizer.visit(g);

            System.out.println("Todas las fases terminadas");
            
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