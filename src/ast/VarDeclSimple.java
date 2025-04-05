package ast;

public class VarDeclSimple extends VarDecl {
	public Type t;
	public Identifier i;

	public VarDeclSimple(Type at, Identifier ai, int ln) {
		super(ln);
		t = at;
		i = ai;
	}
}