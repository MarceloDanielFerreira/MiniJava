package ast.visitor;

import ast.*;

public class ASTPrinterVisitor implements Visitor {
    private int indentLevel = 0;
    
    private void printIndent() {
        for (int i = 0; i < indentLevel; i++) {
            System.out.print("  ");
        }
    }
    
    private void increaseIndent() {
        indentLevel++;
    }
    
    private void decreaseIndent() {
        indentLevel--;
    }

    public void visit(Goal n) {
        printIndent();
        System.out.println("Goal");
        increaseIndent();
        visit(n.m);
        for (int i = 0; i < n.cl.size(); i++) {
            System.out.println();
            visit(n.cl.get(i));
        }
        decreaseIndent();
    }

    public void visit(MainClass n) {
        printIndent();
        System.out.println("MainClass");
        increaseIndent();
        
        printIndent();
        System.out.print("ClassName: ");
        visit(n.i1);
        System.out.println();
        
        printIndent();
        System.out.print("ArgsName: ");
        visit(n.i2);
        System.out.println();
        
        printIndent();
        System.out.println("Variables:");
        increaseIndent();
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
            System.out.println();
        }
        decreaseIndent();
        
        printIndent();
        System.out.println("Statements:");
        increaseIndent();
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
            System.out.println();
        }
        decreaseIndent();
        
        decreaseIndent();
    }

    public void visit(ClassDeclSimple n) {
        printIndent();
        System.out.println("ClassDeclSimple");
        increaseIndent();
        
        printIndent();
        System.out.print("ClassName: ");
        visit(n.i);
        System.out.println();
        
        printIndent();
        System.out.println("Variables:");
        increaseIndent();
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
            System.out.println();
        }
        decreaseIndent();
        
        printIndent();
        System.out.println("Methods:");
        increaseIndent();
        for (int i = 0; i < n.ml.size(); i++) {
            visit(n.ml.get(i));
            System.out.println();
        }
        decreaseIndent();
        
        decreaseIndent();
    }

    public void visit(ClassDeclExtends n) {
        printIndent();
        System.out.println("ClassDeclExtends");
        increaseIndent();
        
        printIndent();
        System.out.print("ClassName: ");
        visit(n.i);
        System.out.println();
        
        printIndent();
        System.out.print("ParentClass: ");
        visit(n.j);
        System.out.println();
        
        printIndent();
        System.out.println("Variables:");
        increaseIndent();
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
            System.out.println();
        }
        decreaseIndent();
        
        printIndent();
        System.out.println("Methods:");
        increaseIndent();
        for (int i = 0; i < n.ml.size(); i++) {
            visit(n.ml.get(i));
            System.out.println();
        }
        decreaseIndent();
        
        decreaseIndent();
    }

    public void visit(VarDeclSimple n) {
        printIndent();
        System.out.print("VarDecl ");
        visit(n.t);
        System.out.print(" ");
        visit(n.i);
        System.out.print(";");
    }
    
    public void visit(VarDeclAssign n) {
        printIndent();
        System.out.print("VarDeclAssign ");
        visit(n.t);
        System.out.print(" ");
        visit(n.i);
        System.out.print(" = ");
        visit(n.e);
        System.out.print(";");
    }

    public void visit(MethodDecl n) {
        printIndent();
        System.out.println("MethodDecl");
        increaseIndent();
        
        printIndent();
        System.out.print("ReturnType: ");
        visit(n.t);
        System.out.println();
        
        printIndent();
        System.out.print("MethodName: ");
        visit(n.i);
        System.out.println();
        
        printIndent();
        System.out.println("Parameters:");
        increaseIndent();
        for (int i = 0; i < n.fl.size(); i++) {
            visit(n.fl.get(i));
            System.out.println();
        }
        decreaseIndent();
        
        printIndent();
        System.out.println("LocalVariables:");
        increaseIndent();
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
            System.out.println();
        }
        decreaseIndent();
        
        printIndent();
        System.out.println("Statements:");
        increaseIndent();
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
            System.out.println();
        }
        decreaseIndent();
        
        printIndent();
        System.out.print("Return: ");
        visit(n.e);
        System.out.println();
        
        decreaseIndent();
    }

    public void visit(Param n) {
        printIndent();
        visit(n.t);
        System.out.print(" ");
        visit(n.i);
    }

    public void visit(IntArrayType n) {
        System.out.print("int[]");
    }

    public void visit(IntType n) {
        System.out.print("int");
    }

    public void visit(ClassType n) {
        System.out.print(n.className);
    }

    public void visit(Block n) {
        printIndent();
        System.out.println("Block");
        increaseIndent();
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
            System.out.println();
        }
        decreaseIndent();
    }

    public void visit(If n) {
        printIndent();
        System.out.println("If");
        increaseIndent();
        
        printIndent();
        System.out.print("Condition: ");
        visit(n.e);
        System.out.println();
        
        printIndent();
        System.out.println("Then:");
        increaseIndent();
        visit(n.s1);
        System.out.println();
        decreaseIndent();
        
        printIndent();
        System.out.println("Else:");
        increaseIndent();
        visit(n.s2);
        System.out.println();
        decreaseIndent();
        
        decreaseIndent();
    }

    public void visit(While n) {
        printIndent();
        System.out.println("While");
        increaseIndent();
        
        printIndent();
        System.out.print("Condition: ");
        visit(n.e);
        System.out.println();
        
        printIndent();
        System.out.println("Body:");
        increaseIndent();
        visit(n.s);
        System.out.println();
        decreaseIndent();
        
        decreaseIndent();
    }

    public void visit(Print n) {
        printIndent();
        System.out.print("Print ");
        visit(n.e);
    }

    public void visit(Assign n) {
        printIndent();
        System.out.print("Assign ");
        visit(n.i);
        System.out.print(" = ");
        visit(n.e);
    }

    public void visit(ArrayAssign n) {
        printIndent();
        System.out.print("ArrayAssign ");
        visit(n.i);
        System.out.print("[");
        visit(n.e1);
        System.out.print("] = ");
        visit(n.e2);
    }

    public void visit(And n) {
        System.out.print("And(");
        visit(n.e1);
        System.out.print(", ");
        visit(n.e2);
        System.out.print(")");
    }
    
    public void visit(Or n) {
        System.out.print("Or(");
        visit(n.e1);
        System.out.print(", ");
        visit(n.e2);
        System.out.print(")");
    }

    public void visit(LessThan n) {
        System.out.print("LessThan(");
        visit(n.e1);
        System.out.print(", ");
        visit(n.e2);
        System.out.print(")");
    }
    
    public void visit(MoreThan n) {
        System.out.print("MoreThan(");
        visit(n.e1);
        System.out.print(", ");
        visit(n.e2);
        System.out.print(")");
    }
    
    public void visit(Equal n) {
        System.out.print("Equal(");
        visit(n.e1);
        System.out.print(", ");
        visit(n.e2);
        System.out.print(")");
    }
    
    public void visit(NotEqual n) {
        System.out.print("NotEqual(");
        visit(n.e1);
        System.out.print(", ");
        visit(n.e2);
        System.out.print(")");
    }

    public void visit(Plus n) {
        System.out.print("Plus(");
        visit(n.e1);
        System.out.print(", ");
        visit(n.e2);
        System.out.print(")");
    }

    public void visit(Minus n) {
        System.out.print("Minus(");
        visit(n.e1);
        System.out.print(", ");
        visit(n.e2);
        System.out.print(")");
    }

    public void visit(Mult n) {
        System.out.print("Mult(");
        visit(n.e1);
        System.out.print(", ");
        visit(n.e2);
        System.out.print(")");
    }
    
    public void visit(Div n) {
        System.out.print("Div(");
        visit(n.e1);
        System.out.print(", ");
        visit(n.e2);
        System.out.print(")");
    }

    public void visit(ArrayLookup n) {
        System.out.print("ArrayLookup(");
        visit(n.e1);
        System.out.print(", ");
        visit(n.e2);
        System.out.print(")");
    }

    public void visit(ArrayLength n) {
        System.out.print("ArrayLength(");
        visit(n.e);
        System.out.print(")");
    }

    public void visit(Call n) {
        System.out.print("Call(");
        visit(n.e);
        System.out.print(", ");
        visit(n.i);
        System.out.print(", [");
        for (int i = 0; i < n.el.size(); i++) {
            visit(n.el.get(i));
            if (i + 1 < n.el.size()) {
                System.out.print(", ");
            }
        }
        System.out.print("])");
    }

    public void visit(IntegerLiteral n) {
        System.out.print("Int(" + n.i + ")");
    }

    public void visit(IdentifierExpr n) {
        System.out.print("Id(" + n.s + ")");
    }

    public void visit(This n) {
        System.out.print("This");
    }

    public void visit(NewArray n) {
        System.out.print("NewArray(");
        visit(n.e);
        System.out.print(")");
    }

    public void visit(NewObject n) {
        System.out.print("NewObject(" + n.i.s + ")");
    }

    public void visit(Identifier n) {
        System.out.print(n.s);
    }
}