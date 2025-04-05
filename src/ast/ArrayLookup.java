package ast;

public class ArrayLookup extends Expr {
	public Expr e1;
	public Expr e2;

	public ArrayLookup(Expr ae1, Expr ae2, int ln) {
		super(ln);
		e1 = ae1;
		e2 = ae2;
	}
}
