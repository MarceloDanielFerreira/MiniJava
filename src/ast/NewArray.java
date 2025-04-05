package ast;

public class NewArray extends Expr {
	public Expr e;

	public NewArray(Expr ae, int ln) {
		super(ln);
		e = ae;
	}

}
