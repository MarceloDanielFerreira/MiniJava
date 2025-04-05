package ast;

import java.util.List;
import java.util.ArrayList;

public class ExprList extends Node {
	private List<Expr> list;

	public ExprList(int ln) {
		super(ln);
		list = new ArrayList<Expr>();
	}

	public void add(Expr n) {
		list.add(n);
	}

	public Expr get(int i) {
		return list.get(i);
	}

	public int size() {
		return list.size();
	}
}
