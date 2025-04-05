package ast;

public class NewObject extends Expr {
	public Identifier i;

	public NewObject(Identifier ai, int ln) {
		super(ln);
		i = ai;
	}

}
