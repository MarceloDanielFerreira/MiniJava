package ast;

public class Call extends Expr {
	public Expr e;
	public Identifier i;
	public ExprList el;

	public Call(Expr ae, Identifier ai, ExprList ael, int ln) {
		super(ln);
		e = ae;
		i = ai;
		el = ael;
	}

}
