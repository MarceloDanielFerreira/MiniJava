package ast;

public class While extends Statement {
	public Expr e;
	public Statement s;

	public While(Expr ae, Statement as, int ln) {
		super(ln);
		e = ae;
		s = as;
	}

}
