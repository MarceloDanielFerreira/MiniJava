package ast;

public class Param extends Node {
	public Type t;
	public Identifier i;

	public Param(Type at, Identifier ai, int ln) {
		super(ln);
		t = at;
		i = ai;
	}
}
