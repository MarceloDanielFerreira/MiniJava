package ast;

public class Assign extends Statement {
	public Identifier i;
	public Expr e;

	public Assign(Identifier ai, Expr ae, int ln) {
		super(ln);
		i = ai;
		e = ae;
	}

}
