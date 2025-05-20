package ast.visitor;

import java.util.Stack;
import ast.*;

public class VariableScopeStack {
    private Stack<VariableScope> stack;
    
    public VariableScopeStack() {
        stack = new Stack<>();
    }
    
    public void pushScope() {
        stack.push(new VariableScope());
    }
    
    public void popScope() {
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }
    
    public void insertSymbol(Type t, Identifier id, Expr expOpt) throws SemanticError {
        if (stack.isEmpty()) {
            throw new SemanticError("No active scope", id.line);
        }
        Variable var = new Variable(t, id, expOpt, id.line);
        stack.peek().addSymbol(id.s, var);
    }
    
    public Variable lookup(String identifier) {
        // Search from top to bottom of the stack
        for (int i = stack.size() - 1; i >= 0; i--) {
            Variable var = stack.get(i).lookup(identifier);
            if (var != null) {
                return var;
            }
        }
        return null;
    }
    
    public VariableScope getCurrentScope() {
        return stack.isEmpty() ? null : stack.peek();
    }
    
    public boolean isEmpty() {
        return stack.isEmpty();
    }
} 