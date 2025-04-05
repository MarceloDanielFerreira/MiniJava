package ast;

public class ClassDeclExtends extends ClassDeclSimple {
	public Identifier j;

	public ClassDeclExtends(Identifier ai, Identifier aj, VarDeclList avl, MethodDeclList aml, int ln) {
		super(ai, avl,aml,ln);
		j = aj;
	}

}
