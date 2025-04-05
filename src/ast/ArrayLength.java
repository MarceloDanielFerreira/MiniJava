package ast;

public class ArrayLength extends Expr {
	public Expr e;

	public ArrayLength(Expr ae, int ln) {
		super(ln);
		e = ae;
	}
}
