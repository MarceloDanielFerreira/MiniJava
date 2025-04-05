package ast;

public class Or extends Expr {
	public Expr e1;
	public Expr e2;

	public Or(int line, Expr e1, Expr e2) {
		super(line);
		this.e1 = e1;
		this.e2 = e2;
	}
}
