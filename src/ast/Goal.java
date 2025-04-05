package ast;

public class Goal extends Node {
	public MainClass m;
	public ClassDeclList cl;

	public Goal(MainClass am, ClassDeclList acl, int ln) {
		super(ln);
		m = am;
		cl = acl;
	}

}
