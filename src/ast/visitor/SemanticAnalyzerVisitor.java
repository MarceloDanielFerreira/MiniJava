package ast.visitor;

import ast.*;
import java.util.*;

public class SemanticAnalyzerVisitor implements Visitor {
    private VariableScopeStack scopeStack;
    private List<SemanticError> errors;
    private Map<String, ClassDecl> classTable;
    private String currentClass;
    private String currentMethod;
    private Type currentMethodReturnType;
    private List<Type> currentMethodParams;
    private boolean inMainClass;

    public SemanticAnalyzerVisitor() {
        this.scopeStack = new VariableScopeStack();
        this.errors = new ArrayList<>();
        this.classTable = new HashMap<>();
        this.currentClass = null;
        this.currentMethod = null;
        this.currentMethodReturnType = null;
        this.currentMethodParams = new ArrayList<>();
        this.inMainClass = false;
    }

    public List<SemanticError> getErrors() {
        // Check for unused variables before returning errors
        checkUnusedVariables();
        return errors;
    }

    private void checkUnusedVariables() {
        VariableScope currentScope = scopeStack.getCurrentScope();
        if (currentScope != null) {
            Map<String, Variable> variables = currentScope.getVariables();
            for (Map.Entry<String, Variable> entry : variables.entrySet()) {
                Variable var = entry.getValue();
                if (!var.used) {
                    addError("Variable '" + var.id.s + "' declared but never used", var.id.line);
                }
            }
        }
    }

    private void addError(String message, int lineNumber) {
        errors.add(new SemanticError(message, lineNumber));
    }

    private boolean isSubtype(Type t1, Type t2) {
        if (t1 instanceof ClassType && t2 instanceof ClassType) {
            String className1 = ((ClassType) t1).className;
            String className2 = ((ClassType) t2).className;
            
            if (className1.equals(className2)) {
                return true;
            }
            
            ClassDecl classDecl = classTable.get(className1);
            while (classDecl instanceof ClassDeclExtends) {
                String parentName = ((ClassDeclExtends) classDecl).j.s;
                if (parentName.equals(className2)) {
                    return true;
                }
                classDecl = classTable.get(parentName);
            }
        }
        return t1.getClass().equals(t2.getClass());
    }

    public void visit(Goal n) {
        // First pass: collect all class declarations
        for (int i = 0; i < n.cl.size(); i++) {
            ClassDecl c = n.cl.get(i);
            if (c instanceof ClassDeclSimple) {
                ClassDeclSimple cs = (ClassDeclSimple) c;
                classTable.put(cs.i.s, cs);
            } else if (c instanceof ClassDeclExtends) {
                ClassDeclExtends ce = (ClassDeclExtends) c;
                classTable.put(ce.i.s, ce);
            }
        }

        // Second pass: analyze
        visit(n.m);
        for (int i = 0; i < n.cl.size(); i++) {
            visit(n.cl.get(i));
        }
    }

    public void visit(ClassDecl c) {
        if (c instanceof ClassDeclExtends) {
            visit((ClassDeclExtends) c);
        } else if (c instanceof ClassDeclSimple) {
            visit((ClassDeclSimple) c);
        }
    }

    public void visit(MainClass n) {
        inMainClass = true;
        currentClass = n.i1.s;
        scopeStack.pushScope();
        
        // Add args parameter - Fix String[] type
        ClassType stringArrayType = new ClassType(n.i2.line, "String[]");
        try {
            insertSymbol(stringArrayType, n.i2, null);
        } catch (SemanticError e) {
            addError(e.getMessage(), e.getLineNumber());
        }
        
        // Visit variables and statements
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
        }
        
        scopeStack.popScope();
        inMainClass = false;
        currentClass = null;
    }

    public void visit(ClassDeclSimple n) {
        currentClass = n.i.s;
        scopeStack.pushScope();
        
        // Visit variables and methods
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.ml.size(); i++) {
            visit(n.ml.get(i));
        }
        
        // Check for unused variables in class scope
        checkUnusedVariables();
        scopeStack.popScope();
        currentClass = null;
    }

    public void visit(ClassDeclExtends n) {
        currentClass = n.i.s;
        scopeStack.pushScope();
        
        // Visit variables and methods
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.ml.size(); i++) {
            visit(n.ml.get(i));
        }
        
        // Check for unused variables in class scope
        checkUnusedVariables();
        scopeStack.popScope();
        currentClass = null;
    }

    public void visit(VarDeclSimple n) {
        try {
            insertSymbol(n.t, n.i, null);
        } catch (SemanticError e) {
            addError(e.getMessage(), e.getLineNumber());
        }
    }

    public void visit(VarDeclAssign n) {
        try {
            insertSymbol(n.t, n.i, n.e);
            Type exprType = getExpressionType(n.e);
            if (!isSubtype(exprType, n.t)) {
                addError("Type mismatch in variable assignment", n.i.line);
            }
        } catch (SemanticError e) {
            addError(e.getMessage(), e.getLineNumber());
        }
    }

    public void visit(MethodDecl n) {
        currentMethod = n.i.s;
        currentMethodReturnType = n.t;
        currentMethodParams = new ArrayList<>();
        
        scopeStack.pushScope();
        
        // Add parameters to scope
        for (int i = 0; i < n.fl.size(); i++) {
            Param p = n.fl.get(i);
            currentMethodParams.add(p.t);
            try {
                insertSymbol(p.t, p.i, null);
            } catch (SemanticError e) {
                addError(e.getMessage(), e.getLineNumber());
            }
        }
        
        // Visit variables and statements
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
        }
        
        // Check return type
        Type returnType = getExpressionType(n.e);
        if (!isSubtype(returnType, currentMethodReturnType)) {
            addError("Return type mismatch in method " + currentMethod, n.e.line);
        }
        
        // Mark parameters as used if they are used in the method body
        for (int i = 0; i < n.fl.size(); i++) {
            Param p = n.fl.get(i);
            Variable var = scopeStack.lookup(p.i.s);
            if (var != null) {
                var.used = true;
            }
        }
        
        // Check for unused variables in method scope
        checkUnusedVariables();
        scopeStack.popScope();
        currentMethod = null;
        currentMethodReturnType = null;
        currentMethodParams = null;
    }

    public void visit(Param n) {
        // Handled in MethodDecl
    }

    public void visit(IntArrayType n) {
        // No action needed
    }

    public void visit(IntType n) {
        // No action needed
    }

    public void visit(ClassType n) {
        // No action needed
    }

    public void visit(Block n) {
        scopeStack.pushScope();
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
        }
        scopeStack.popScope();
    }

    public void visit(If n) {
        Type condType = getExpressionType(n.e);
        if (!(condType instanceof IntType)) {
            addError("Condition must be of type int", n.e.line);
        }
        visit(n.s1);
        visit(n.s2);
    }

    public void visit(While n) {
        Type condType = getExpressionType(n.e);
        if (!(condType instanceof IntType)) {
            addError("Condition must be of type int", n.e.line);
        }
        visit(n.s);
    }

    public void visit(Print n) {
        Type exprType = getExpressionType(n.e);
        if (!(exprType instanceof IntType)) {
            addError("Print expression must be of type int", n.e.line);
        }
    }

    public void visit(Assign n) {
        Variable var = scopeStack.lookup(n.i.s);
        if (var == null) {
            addError("Variable " + n.i.s + " not declared", n.i.line);
            return;
        }
        
        Type exprType = getExpressionType(n.e);
        if (!isSubtype(exprType, var.type)) {
            addError("Type mismatch in assignment", n.i.line);
        }
        
        // Mark variables used in the expression as used
        if (n.e instanceof IdentifierExpr) {
            Variable exprVar = scopeStack.lookup(((IdentifierExpr) n.e).s);
            if (exprVar != null) {
                exprVar.used = true;
            }
        }
    }

    public void visit(ArrayAssign n) {
        Variable var = scopeStack.lookup(n.i.s);
        if (var == null) {
            addError("Variable " + n.i.s + " not declared", n.i.line);
            return;
        }
        
        if (!(var.type instanceof IntArrayType)) {
            addError("Variable " + n.i.s + " is not an array", n.i.line);
            return;
        }
        
        Type indexType = getExpressionType(n.e1);
        if (!(indexType instanceof IntType)) {
            addError("Array index must be of type int", n.e1.line);
        }
        
        Type valueType = getExpressionType(n.e2);
        if (!(valueType instanceof IntType)) {
            addError("Array element must be of type int", n.e2.line);
        }
        
        // Mark variables used in expressions as used
        if (n.e1 instanceof IdentifierExpr) {
            Variable exprVar = scopeStack.lookup(((IdentifierExpr) n.e1).s);
            if (exprVar != null) {
                exprVar.used = true;
            }
        }
        if (n.e2 instanceof IdentifierExpr) {
            Variable exprVar = scopeStack.lookup(((IdentifierExpr) n.e2).s);
            if (exprVar != null) {
                exprVar.used = true;
            }
        }
    }

    public void visit(And n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("And operands must be of type int", n.e1.line);
        }
    }

    public void visit(Or n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Or operands must be of type int", n.e1.line);
        }
    }

    public void visit(LessThan n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("LessThan operands must be of type int", n.e1.line);
        }
    }

    public void visit(MoreThan n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("MoreThan operands must be of type int", n.e1.line);
        }
    }

    public void visit(Equal n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!isSubtype(t1, t2) && !isSubtype(t2, t1)) {
            addError("Equal operands must be of compatible types", n.e1.line);
        }
    }

    public void visit(NotEqual n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!isSubtype(t1, t2) && !isSubtype(t2, t1)) {
            addError("NotEqual operands must be of compatible types", n.e1.line);
        }
    }

    public void visit(Plus n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Plus operands must be of type int", n.e1.line);
        }
    }

    public void visit(Minus n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Minus operands must be of type int", n.e1.line);
        }
    }

    public void visit(Mult n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Mult operands must be of type int", n.e1.line);
        }
    }

    public void visit(Div n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Div operands must be of type int", n.e1.line);
        }
    }

    public void visit(ArrayLookup n) {
        Type arrayType = getExpressionType(n.e1);
        Type indexType = getExpressionType(n.e2);
        
        if (!(arrayType instanceof IntArrayType)) {
            addError("Array lookup must be performed on an array", n.e1.line);
        }
        if (!(indexType instanceof IntType)) {
            addError("Array index must be of type int", n.e2.line);
        }
        
        // Mark variables used in array lookup as used
        if (n.e1 instanceof IdentifierExpr) {
            Variable exprVar = scopeStack.lookup(((IdentifierExpr) n.e1).s);
            if (exprVar != null) {
                exprVar.used = true;
            }
        }
        if (n.e2 instanceof IdentifierExpr) {
            Variable exprVar = scopeStack.lookup(((IdentifierExpr) n.e2).s);
            if (exprVar != null) {
                exprVar.used = true;
            }
        }
    }

    public void visit(ArrayLength n) {
        Type arrayType = getExpressionType(n.e);
        if (!(arrayType instanceof IntArrayType)) {
            addError("Length can only be accessed on arrays", n.e.line);
        }
    }

    public void visit(Call n) {
        Type objType = getExpressionType(n.e);
        if (!(objType instanceof ClassType)) {
            addError("Method call must be on an object", n.e.line);
            return;
        }
        
        String className = ((ClassType) objType).className;
        ClassDecl classDecl = classTable.get(className);
        if (classDecl == null) {
            addError("Class " + className + " not found", n.e.line);
            return;
        }
        
        // Find method in class hierarchy
        MethodDecl method = null;
        while (classDecl != null) {
            if (classDecl instanceof ClassDeclSimple) {
                ClassDeclSimple cs = (ClassDeclSimple) classDecl;
                for (int i = 0; i < cs.ml.size(); i++) {
                    MethodDecl m = cs.ml.get(i);
                    if (m.i.s.equals(n.i.s)) {
                        method = m;
                        break;
                    }
                }
            } else if (classDecl instanceof ClassDeclExtends) {
                ClassDeclExtends ce = (ClassDeclExtends) classDecl;
                for (int i = 0; i < ce.ml.size(); i++) {
                    MethodDecl m = ce.ml.get(i);
                    if (m.i.s.equals(n.i.s)) {
                        method = m;
                        break;
                    }
                }
                classDecl = classTable.get(ce.j.s);
            }
            if (method != null) break;
        }
        
        if (method == null) {
            addError("Method " + n.i.s + " not found in class " + className, n.i.line);
            return;
        }
        
        // Check number of arguments
        if (n.el.size() != method.fl.size()) {
            addError("Wrong number of arguments in method call", n.i.line);
            return;
        }
        
        // Check argument types
        for (int i = 0; i < n.el.size(); i++) {
            Type argType = getExpressionType(n.el.get(i));
            Type paramType = method.fl.get(i).t;
            if (!isSubtype(argType, paramType)) {
                addError("Argument type mismatch in method call", n.el.get(i).line);
            }
        }
    }

    public void visit(IntegerLiteral n) {
        // No action needed
    }

    public void visit(IdentifierExpr n) {
        Variable var = scopeStack.lookup(n.s);
        if (var == null) {
            addError("Variable " + n.s + " not declared", n.line);
            return;
        }
        // Mark variable as used when it appears in an expression (being read)
        var.used = true;
    }

    public void visit(This n) {
        if (currentClass == null) {
            addError("'this' cannot be used in static context", n.line);
        }
    }

    public void visit(NewArray n) {
        Type sizeType = getExpressionType(n.e);
        if (!(sizeType instanceof IntType)) {
            addError("Array size must be of type int", n.e.line);
        }
    }

    public void visit(NewObject n) {
        if (!classTable.containsKey(n.i.s)) {
            addError("Class " + n.i.s + " not found", n.i.line);
        }
    }

    public void visit(Identifier n) {
        // No action needed
    }

    private Type getExpressionType(Expr e) {
        if (e == null) {
            return null;
        }
        
        if (e instanceof IntegerLiteral) {
            return new IntType(e.line);
        } else if (e instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) e).s);
            return var != null ? var.type : null;
        } else if (e instanceof This) {
            if (currentClass == null) {
                return null;
            }
            return new ClassType(e.line, currentClass);
        } else if (e instanceof NewArray) {
            return new IntArrayType(e.line);
        } else if (e instanceof NewObject) {
            return new ClassType(e.line, ((NewObject) e).i.s);
        } else if (e instanceof ArrayLength) {
            return new IntType(e.line);
        } else if (e instanceof ArrayLookup) {
            return new IntType(e.line);
        } else if (e instanceof Call) {
            // Find method return type
            Type objType = getExpressionType(((Call) e).e);
            if (objType instanceof ClassType) {
                String className = ((ClassType) objType).className;
                ClassDecl classDecl = classTable.get(className);
                while (classDecl != null) {
                    if (classDecl instanceof ClassDeclSimple) {
                        ClassDeclSimple cs = (ClassDeclSimple) classDecl;
                        for (int i = 0; i < cs.ml.size(); i++) {
                            MethodDecl m = cs.ml.get(i);
                            if (m.i.s.equals(((Call) e).i.s)) {
                                return m.t;
                            }
                        }
                    } else if (classDecl instanceof ClassDeclExtends) {
                        ClassDeclExtends ce = (ClassDeclExtends) classDecl;
                        for (int i = 0; i < ce.ml.size(); i++) {
                            MethodDecl m = ce.ml.get(i);
                            if (m.i.s.equals(((Call) e).i.s)) {
                                return m.t;
                            }
                        }
                        classDecl = classTable.get(ce.j.s);
                    }
                }
            }
            return null;
        } else if (e instanceof And || e instanceof Or || e instanceof LessThan || 
                  e instanceof MoreThan || e instanceof Equal || e instanceof NotEqual ||
                  e instanceof Plus || e instanceof Minus || e instanceof Mult || 
                  e instanceof Div) {
            return new IntType(e.line);
        }
        return null;
    }

    private void insertSymbol(Type t, Identifier id, Expr expOpt) throws SemanticError {
        scopeStack.insertSymbol(t, id, expOpt);
    }
} 