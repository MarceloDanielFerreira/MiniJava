import java.io.InputStreamReader;
import ast.Goal;
import ast.visitor.MiniJPrintVisitor;
import ast.visitor.Visitor;
import java_cup.runtime.Symbol;

public class Main {

	public static void main(String[] args) {
		InputStreamReader isr = new InputStreamReader(System.in);
		Scanner s = new Scanner(isr);
		parser p = new parser(s);
		try {
			Symbol root = p.parse();
			Visitor mj = new MiniJPrintVisitor();
			Goal g = (Goal) root.value;
			mj.visit(g);

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