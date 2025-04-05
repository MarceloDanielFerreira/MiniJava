package ast;

public class MainClass extends Node {
	public Identifier i1;
	public Identifier i2;
	public VarDeclList vl;
	public StatementList sl;

	public MainClass(Identifier ai1, Identifier ai2, VarDeclList avl, StatementList asl, int ln) {
		super(ln);
		i1 = ai1;
		i2 = ai2;
		vl = avl;
		sl = asl;
	}

}
