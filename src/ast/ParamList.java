package ast;

import java.util.List;
import java.util.ArrayList;

public class ParamList extends Node {
	private List<Param> list;

	public ParamList(int ln) {
		super(ln);
		list = new ArrayList<Param>();
	}

	public void add(Param n) {
		list.add(n);
	}

	public Param get(int i) {
		return list.get(i);
	}

	public int size() {
		return list.size();
	}
}
