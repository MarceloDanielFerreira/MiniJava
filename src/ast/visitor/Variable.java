package ast.visitor;

import ast.*;

public class Variable {
    public Type type;
    public Identifier id;
    public Expr expOpt;
    public boolean used;
    private boolean initialized;
    private int lineNumber;

    public Variable(Type type, Identifier id, Expr expOpt, int lineNumber) {
        this.type = type;
        this.id = id;
        this.expOpt = expOpt;
        this.used = false;
        this.initialized = expOpt != null;
        this.lineNumber = lineNumber;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public int getLineNumber() {
        return lineNumber;
    }
} 