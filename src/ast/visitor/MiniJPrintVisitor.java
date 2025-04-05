package ast.visitor;

import ast.*;

public class MiniJPrintVisitor implements Visitor {

	public void visit(Goal n) {
		visit(n.m);
		for (int i = 0; i < n.cl.size(); i++) {
			System.out.println();
			visit(n.cl.get(i));
		}
	}

	public void visit(MainClass n) {
		System.out.print("class ");
		visit(n.i1);
		System.out.println(" {");
		System.out.print("  public static void main (String [] ");
		visit(n.i2);
		System.out.println(") {");
		System.out.print("    ");
		for (int i = 0; i < n.vl.size(); i++) {
			System.out.print("    ");
			visit(n.vl.get(i));
			System.out.println("");
		}
		for (int i = 0; i < n.sl.size(); i++) {
			System.out.print("    ");
			visit(n.sl.get(i));
			if (i < n.sl.size()) {
				System.out.println("");
			}
		}
		System.out.println("  }");
		System.out.println("}");
	}

	public void visit(ClassDeclSimple n) {
		System.out.print("class ");
		visit(n.i);
		System.out.println(" { ");
		for (int i = 0; i < n.vl.size(); i++) {
			System.out.print("  ");
			visit(n.vl.get(i));
			if (i + 1 < n.vl.size()) {
				System.out.println();
			}
		}
		for (int i = 0; i < n.ml.size(); i++) {
			System.out.println();
			visit(n.ml.get(i));
		}
		System.out.println();
		System.out.println("}");
	}

	public void visit(ClassDeclExtends n) {
		System.out.print("class ");
		visit(n.i);
		System.out.println(" extends ");
		visit(n.j);
		System.out.println(" { ");
		for (int i = 0; i < n.vl.size(); i++) {
			System.out.print("  ");
			visit(n.vl.get(i));
			if (i + 1 < n.vl.size()) {
				System.out.println();
			}
		}
		for (int i = 0; i < n.ml.size(); i++) {
			System.out.println();
			visit(n.ml.get(i));
		}
		System.out.println();
		System.out.println("}");
	}

	public void visit(VarDeclSimple n) {
		visit(n.t);
		System.out.print(" ");
		visit(n.i);
		System.out.print(";");
	}
	
	public void visit(VarDeclAssign n) {
		visit(n.t);
		System.out.print(" ");
		visit(n.i);
		System.out.print(" = ");
		visit(n.e);
		System.out.print(";");
	}

	public void visit(MethodDecl n) {
		System.out.print("  public ");
		visit(n.t);
		System.out.print(" ");
		visit(n.i);
		System.out.print(" (");
		for (int i = 0; i < n.fl.size(); i++) {
			visit(n.fl.get(i));
			if (i + 1 < n.fl.size()) {
				System.out.print(", ");
			}
		}
		System.out.println(") { ");
		for (int i = 0; i < n.vl.size(); i++) {
			System.out.print("    ");
			visit(n.vl.get(i));
			System.out.println("");
		}
		for (int i = 0; i < n.sl.size(); i++) {
			System.out.print("    ");
			visit(n.sl.get(i));
			if (i < n.sl.size()) {
				System.out.println("");
			}
		}
		System.out.print("    return ");
		visit(n.e);
		System.out.println(";");
		System.out.print("  }");
	}

	public void visit(Param n) {
		visit(n.t);
		System.out.print(" ");
		visit(n.i);
	}

	public void visit(IntArrayType n) {
		System.out.print("int []");
	}

	public void visit(IntType n) {
		System.out.print("int");
	}

	public void visit(ClassType n) {
		System.out.print(n.className);
	}

	public void visit(Block n) {
		System.out.println("{ ");
		for (int i = 0; i < n.sl.size(); i++) {
			System.out.print("      ");
			visit(n.sl.get(i));
			System.out.println();
		}
		System.out.print("    } ");
	}

	public void visit(If n) {
		System.out.print("if (");
		visit(n.e);
		System.out.println(") ");
		System.out.print("    ");
		visit(n.s1);
		System.out.println();
		System.out.print("    else ");
		visit(n.s2);
	}

	public void visit(While n) {
		System.out.print("while (");
		visit(n.e);
		System.out.print(") ");
		visit(n.s);
	}

	public void visit(Print n) {
		System.out.print("System.out.println(");
		visit(n.e);
		System.out.print(");");
	}

	public void visit(Assign n) {
		visit(n.i);
		System.out.print(" = ");
		visit(n.e);
		System.out.print(";");
	}

	public void visit(ArrayAssign n) {
		visit(n.i);
		System.out.print("[");
		visit(n.e1);
		System.out.print("] = ");
		visit(n.e2);
		System.out.print(";");
	}

	public void visit(And n) {
		System.out.print("(");
		visit(n.e1);
		System.out.print(" && ");
		visit(n.e2);
		System.out.print(")");
	}
	
	public void visit(Or n) {
		System.out.print("(");
		visit(n.e1);
		System.out.print(" || ");
		visit(n.e2);
		System.out.print(")");
	}

	public void visit(LessThan n) {
		System.out.print("(");
		visit(n.e1);
		System.out.print(" < ");
		visit(n.e2);
		System.out.print(")");
	}
	
	public void visit(MoreThan n) {
		System.out.print("(");
		visit(n.e1);
		System.out.print(" > ");
		visit(n.e2);
		System.out.print(")");
	}
	
	public void visit(Equal n) {
		System.out.print("(");
		visit(n.e1);
		System.out.print(" == ");
		visit(n.e2);
		System.out.print(")");
	}
	
	public void visit(NotEqual n) {
		System.out.print("(");
		visit(n.e1);
		System.out.print(" != ");
		visit(n.e2);
		System.out.print(")");
	}

	public void visit(Plus n) {
		System.out.print("(");
		visit(n.e1);
		System.out.print(" + ");
		visit(n.e2);
		System.out.print(")");
	}

	public void visit(Minus n) {
		System.out.print("(");
		visit(n.e1);
		System.out.print(" - ");
		visit(n.e2);
		System.out.print(")");
	}

	public void visit(Mult n) {
		System.out.print("(");
		visit(n.e1);
		System.out.print(" * ");
		visit(n.e2);
		System.out.print(")");
	}
	
	public void visit(Div n) {
		System.out.print("(");
		visit(n.e1);
		System.out.print(" / ");
		visit(n.e2);
		System.out.print(")");
	}

	public void visit(ArrayLookup n) {
		visit(n.e1);
		System.out.print("[");
		visit(n.e2);
		System.out.print("]");
	}

	public void visit(ArrayLength n) {
		visit(n.e);
		System.out.print(".length");
	}

	public void visit(Call n) {
		visit(n.e);
		System.out.print(".");
		visit(n.i);
		System.out.print("(");
		for (int i = 0; i < n.el.size(); i++) {
			visit(n.el.get(i));
			if (i + 1 < n.el.size()) {
				System.out.print(", ");
			}
		}
		System.out.print(")");
	}

	public void visit(IntegerLiteral n) {
		System.out.print(n.i);
	}

	public void visit(IdentifierExpr n) {
		System.out.print(n.s);
	}

	public void visit(This n) {
		System.out.print("this");
	}

	public void visit(NewArray n) {
		System.out.print("new int [");
		visit(n.e);
		System.out.print("]");
	}

	public void visit(NewObject n) {
		System.out.print("new ");
		System.out.print(n.i.s);
		System.out.print("()");
	}


	public void visit(Identifier n) {
		System.out.print(n.s);
	}
}