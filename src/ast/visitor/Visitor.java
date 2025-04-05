package ast.visitor;

import ast.*;

public interface Visitor {
	public void visit(Goal n);

	public void visit(MainClass n);

	public void visit(ClassDeclSimple n);

	public void visit(ClassDeclExtends n);

	public void visit(VarDeclSimple n);
	
	public void visit(VarDeclAssign n);

	public void visit(MethodDecl n);

	public void visit(Param n);

	public void visit(IntArrayType n);

	public void visit(IntType n);

	public void visit(ClassType n);

	public void visit(Block n);

	public void visit(If n);

	public void visit(While n);

	public void visit(Print n);

	public void visit(Assign n);

	public void visit(ArrayAssign n);

	public void visit(And n);
	public void visit(Or n);
	
	public void visit(Equal n);
	public void visit(NotEqual n);
	public void visit(LessThan n);
	public void visit(MoreThan n);

	public void visit(Plus n);
	public void visit(Minus n);
	public void visit(Mult n);
	public void visit(Div n);


	public void visit(ArrayLookup n);

	public void visit(ArrayLength n);

	public void visit(Call n);

	public void visit(IntegerLiteral n);

	public void visit(IdentifierExpr n);

	public void visit(This n);

	public void visit(NewArray n);

	public void visit(NewObject n);

	public void visit(Identifier n);

	public default void visit(ClassDecl c) {
		// Hack, we could resolve this with OO
		if (c instanceof ClassDeclExtends) {
			visit((ClassDeclExtends) c);
		} else if (c instanceof ClassDeclSimple) {
			visit((ClassDeclSimple) c);
		}

	}

	public default void visit(Expr e) {
		if (e instanceof And) {
			visit((And) e);
		} else if (e instanceof Or) {
			visit((Or) e);
		} else if (e instanceof ArrayLength) {
			visit((ArrayLength) e);
		} else if (e instanceof ArrayLookup) {
			visit((ArrayLookup) e);
		} else if (e instanceof Call) {
			visit((Call) e);
		} else if (e instanceof IdentifierExpr) {
			visit((IdentifierExpr) e);
		} else if (e instanceof IntegerLiteral) {
			visit((IntegerLiteral) e);
		} else if (e instanceof LessThan) {
			visit((LessThan) e);
		} else if (e instanceof Equal) {
			visit((Equal) e);
		} else if (e instanceof NotEqual) {
			visit((NotEqual) e);
		} else if (e instanceof MoreThan) {
			visit((MoreThan) e);
		} else if (e instanceof Plus) {
			visit((Plus) e);
		} else if (e instanceof Minus) {
			visit((Minus) e);
		} else if (e instanceof Mult) {
			visit((Mult) e);
		} else if (e instanceof Div) {
			visit((Div) e);
		} else if (e instanceof NewArray) {
			visit((NewArray) e);
		} else if (e instanceof NewObject) {
			visit((NewObject) e);
		} else if (e instanceof This) {
			visit((This) e);
		}
	}

	public default void visit(Type e) {
		if (e instanceof IntType) {
			visit((IntType) e);
		} else if (e instanceof IntArrayType) {
			visit((IntArrayType) e);
		} else if (e instanceof ClassType) {
			visit((ClassType) e);
		}
	}
	
	public default void visit(VarDecl e) {
		if( e instanceof VarDeclAssign) {
			visit((VarDeclAssign)e);
		} else if (e instanceof VarDeclSimple) {
			visit((VarDeclSimple)e);
		} 
	}

	public default void visit(Statement s) {
		if (s instanceof ArrayAssign) {
			visit((ArrayAssign) s);
		} else if (s instanceof Assign) {
			visit((Assign) s);
		} else if (s instanceof Block) {
			visit((Block) s);
		} else if (s instanceof If) {
			visit((If) s);
		} else if (s instanceof Print) {
			visit((Print) s);
		} else if (s instanceof While) {
			visit((While) s);
		}
	}
}