package ast;

public class Print extends Statement {
	public Expr e;

	public Print(Expr ae, int ln) {
		super(ln);
		e = ae;
	}
}
