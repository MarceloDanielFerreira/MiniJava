package ast.visitor;

import java.util.HashMap;
import java.util.Map;

public class VariableScope {
    private Map<String, Variable> varSet;
    
    public VariableScope() {
        varSet = new HashMap<>();
    }
    
    public void addSymbol(String id, Variable var) throws SemanticError {
        if (varSet.containsKey(id)) {
            throw new SemanticError("Variable '" + id + "' ya declarada en este ámbito", var.getLineNumber());
        }
        varSet.put(id, var);
    }
    
    public Variable lookup(String id) {
        return varSet.get(id);
    }
    
    public boolean contains(String id) {
        return varSet.containsKey(id);
    }
    
    public Map<String, Variable> getVariables() {
        return varSet;
    }
} 