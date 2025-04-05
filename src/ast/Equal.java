package ast;

public class Equal extends Expr {
	public Expr e1;
	public Expr e2;

	public Equal(int line, Expr e1, Expr e2) {
		super(line);
		this.e1 = e1;
		this.e2 = e2;
	}
}
