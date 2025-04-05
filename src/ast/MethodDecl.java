package ast;

public class MethodDecl extends Node {
	public Type t;
	public Identifier i;
	public ParamList fl;
	public VarDeclList vl;
	public StatementList sl;
	public Expr e;

	public MethodDecl(Type at, Identifier ai, ParamList afl, VarDeclList avl, StatementList asl, Expr ae, int ln) {
		super(ln);
		t = at;
		i = ai;
		fl = afl;
		vl = avl;
		sl = asl;
		e = ae;
	}

}
