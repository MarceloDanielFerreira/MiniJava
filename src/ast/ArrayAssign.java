package ast;

public class ArrayAssign extends Statement {
	public Identifier i;
	public Expr e1;
	public Expr e2;

	public ArrayAssign(Identifier ai, Expr ae1, Expr ae2, int ln) {
		super(ln);
		i = ai;
		e1 = ae1;
		e2 = ae2;
	}
}
