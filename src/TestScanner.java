import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import ast.ClassDeclSimple;
import ast.Goal;
import ast.Statement;
import ast.visitor.MiniJPrintVisitor;
import ast.visitor.Visitor;
import java_cup.runtime.Symbol;

public class TestScanner {

  public static void main(String[] args) {
    InputStreamReader isr = new InputStreamReader(System.in);
    Scanner s = new Scanner(isr);
    Symbol t;
    try {
    
      t = s.next_token();
      while (t.sym != sym.EOF) {
        System.out.print(" Token " + t.toString() + " " + t.value + "\n");
        t = s.next_token();
      }
      isr.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}