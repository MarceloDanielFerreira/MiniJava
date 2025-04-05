package ast;

public class IntegerLiteral extends Expr {
	public int i;

	public IntegerLiteral(int ai, int ln) {
		super(ln);
		i = ai;
	}

}
