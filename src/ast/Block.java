package ast;

public class Block extends Statement {
	public StatementList sl;

	public Block(StatementList asl, int ln) {
		super(ln);
		sl = asl;
	}

}
