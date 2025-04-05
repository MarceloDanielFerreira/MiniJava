package ast;

public class Identifier extends Node {
	public String s;

	public Identifier(String as, int ln) {
		super(ln);
		s = as;
	}

	public String toString() {
		return s;
	}
}
