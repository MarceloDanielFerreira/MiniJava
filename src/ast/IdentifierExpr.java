package ast;

public class IdentifierExpr extends Expr {
	public String s;

	public IdentifierExpr(String as, int ln) {
		super(ln);
		s = as;
	}

}
