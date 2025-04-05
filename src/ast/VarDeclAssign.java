package ast;

public class VarDeclAssign extends VarDeclSimple {
	public Expr e;

	public VarDeclAssign(Type at, Identifier ai, Expr ae, int ln) {
		super(at, ai, ln);
		e = ae;
	}
}