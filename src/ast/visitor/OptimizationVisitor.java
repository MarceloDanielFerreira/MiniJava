package ast.visitor;

import ast.*;
import java.util.*;

/**
 * Visitante que optimiza el AST eliminando variables no utilizadas.
 * Este visitante recorre el árbol sintáctico abstracto (AST) y elimina
 * las variables que son declaradas pero nunca utilizadas en el código.
 */
public class OptimizationVisitor implements Visitor {
    // Pila de ámbitos para manejar el scope de variables
    private VariableScopeStack scopeStack;
    // Mapa que registra qué variables han sido utilizadas
    private Map<String, Boolean> variablesUsadas;

    /**
     * Constructor del visitante de optimización.
     * Inicializa la pila de ámbitos y el mapa de variables utilizadas.
     */
    public OptimizationVisitor() {
        this.scopeStack = new VariableScopeStack();
        this.variablesUsadas = new HashMap<>();
    }

    /**
     * Visita el nodo Goal (programa completo).
     * Recorre la clase main y todas las clases del programa.
     */
    public void visit(Goal n) {
        visit(n.m);
        for (int i = 0; i < n.cl.size(); i++) {
            visit(n.cl.get(i));
        }
    }

    /**
     * Visita la clase main.
     * Recorre sus variables y sentencias, y elimina las variables no utilizadas.
     */
    public void visit(MainClass n) {
        scopeStack.pushScope();
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
        }
        eliminarVariablesNoUsadas(n.vl);
        scopeStack.popScope();
    }

    /**
     * Visita una clase simple (sin herencia).
     * Recorre sus variables y métodos, y elimina las variables no utilizadas.
     */
    public void visit(ClassDeclSimple n) {
        scopeStack.pushScope();
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.ml.size(); i++) {
            visit(n.ml.get(i));
        }
        eliminarVariablesNoUsadas(n.vl);
        scopeStack.popScope();
    }

    /**
     * Visita una clase con herencia.
     * Recorre sus variables y métodos, y elimina las variables no utilizadas.
     */
    public void visit(ClassDeclExtends n) {
        scopeStack.pushScope();
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.ml.size(); i++) {
            visit(n.ml.get(i));
        }
        eliminarVariablesNoUsadas(n.vl);
        scopeStack.popScope();
    }

    /**
     * Visita una declaración de variable simple.
     * Registra la variable en el ámbito actual.
     */
    public void visit(VarDeclSimple n) {
        try {
            scopeStack.insertSymbol(n.t, n.i, null);
        } catch (SemanticError e) {
            // Ignorar errores semánticos en la optimización
        }
    }

    /**
     * Visita una declaración de variable con asignación.
     * Registra la variable y visita su expresión de inicialización.
     */
    public void visit(VarDeclAssign n) {
        try {
            scopeStack.insertSymbol(n.t, n.i, n.e);
            visit(n.e);
        } catch (SemanticError e) {
            // Ignorar errores semánticos en la optimización
        }
    }

    /**
     * Visita una declaración de método.
     * Recorre sus parámetros, variables locales y sentencias.
     * Marca los parámetros como utilizados por defecto.
     */
    public void visit(MethodDecl n) {
        scopeStack.pushScope();
        
        // Agregar parámetros al ámbito
        for (int i = 0; i < n.fl.size(); i++) {
            Param p = n.fl.get(i);
            try {
                scopeStack.insertSymbol(p.t, p.i, null);
                variablesUsadas.put(p.i.s, true); // Los parámetros siempre se consideran usados
            } catch (SemanticError e) {
                // Ignorar errores semánticos en la optimización
            }
        }
        
        // Visitar variables y sentencias
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
        }
        visit(n.e);
        
        eliminarVariablesNoUsadas(n.vl);
        scopeStack.popScope();
    }

    /**
     * Elimina las variables no utilizadas de una lista de declaraciones.
     * @param vl Lista de declaraciones de variables a analizar
     */
    private void eliminarVariablesNoUsadas(VarDeclList vl) {
        for (int i = vl.size() - 1; i >= 0; i--) {
            VarDecl vd = vl.get(i);
            String nombreVariable = vd instanceof VarDeclSimple ? 
                ((VarDeclSimple) vd).i.s : 
                ((VarDeclAssign) vd).i.s;
            
            if (!variablesUsadas.getOrDefault(nombreVariable, false)) {
                String tipo = vd instanceof VarDeclSimple ? 
                    ((VarDeclSimple) vd).t.getClass().getSimpleName() :
                    ((VarDeclAssign) vd).t.getClass().getSimpleName();
                System.out.println("Optimizacion: Eliminando variable no utilizada '" + nombreVariable + "' de tipo " + tipo);
                vl.remove(i);
            }
        }
    }

    /**
     * Visita un bloque de código.
     * Crea un nuevo ámbito para las variables locales.
     */
    public void visit(Block n) {
        scopeStack.pushScope();
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
        }
        scopeStack.popScope();
    }

    /**
     * Visita una sentencia if.
     * Recorre la condición y ambos bloques.
     */
    public void visit(If n) {
        visit(n.e);
        visit(n.s1);
        visit(n.s2);
    }

    /**
     * Visita una sentencia while.
     * Recorre la condición y el cuerpo del bucle.
     */
    public void visit(While n) {
        visit(n.e);
        visit(n.s);
    }

    /**
     * Visita una sentencia print.
     * Recorre la expresión a imprimir.
     */
    public void visit(Print n) {
        visit(n.e);
    }

    /**
     * Visita una asignación.
     * Recorre la expresión y marca la variable como usada si está en el ámbito actual.
     */
    public void visit(Assign n) {
        // Visitar primero la expresión para marcar variables usadas en ella
        visit(n.e);
        
        // Marcar la variable como usada solo si está en el ámbito actual
        // y si su valor se usa en alguna expresión posterior
        Variable var = scopeStack.lookup(n.i.s);
        if (var != null) {
            // No marcar como usada aquí, solo se marcará si se usa en una expresión
            // Esto permite detectar variables asignadas pero nunca usadas
        }
    }

    /**
     * Visita una asignación a array.
     * Recorre las expresiones y marca la variable del array como usada.
     */
    public void visit(ArrayAssign n) {
        // Visitar primero las expresiones para marcar variables usadas en ellas
        visit(n.e1);
        visit(n.e2);
        
        // Marcar la variable del array como usada
        Variable var = scopeStack.lookup(n.i.s);
        if (var != null) {
            variablesUsadas.put(n.i.s, true);
        }
    }

    /**
     * Visita una operación lógica AND.
     * Recorre ambos operandos.
     */
    public void visit(And n) {
        visit(n.e1);
        visit(n.e2);
    }

    /**
     * Visita una operación lógica OR.
     * Recorre ambos operandos.
     */
    public void visit(Or n) {
        visit(n.e1);
        visit(n.e2);
    }

    /**
     * Visita una comparación menor que.
     * Recorre ambos operandos.
     */
    public void visit(LessThan n) {
        visit(n.e1);
        visit(n.e2);
    }

    /**
     * Visita una comparación mayor que.
     * Recorre ambos operandos.
     */
    public void visit(MoreThan n) {
        visit(n.e1);
        visit(n.e2);
    }

    /**
     * Visita una comparación de igualdad.
     * Recorre ambos operandos.
     */
    public void visit(Equal n) {
        visit(n.e1);
        visit(n.e2);
    }

    /**
     * Visita una comparación de desigualdad.
     * Recorre ambos operandos.
     */
    public void visit(NotEqual n) {
        visit(n.e1);
        visit(n.e2);
    }

    /**
     * Visita una suma.
     * Recorre ambos operandos.
     */
    public void visit(Plus n) {
        visit(n.e1);
        visit(n.e2);
    }

    /**
     * Visita una resta.
     * Recorre ambos operandos.
     */
    public void visit(Minus n) {
        visit(n.e1);
        visit(n.e2);
    }

    /**
     * Visita una multiplicación.
     * Recorre ambos operandos.
     */
    public void visit(Mult n) {
        visit(n.e1);
        visit(n.e2);
    }

    /**
     * Visita una división.
     * Recorre ambos operandos.
     */
    public void visit(Div n) {
        visit(n.e1);
        visit(n.e2);
    }

    /**
     * Visita un acceso a array.
     * Recorre la expresión del array y el índice.
     */
    public void visit(ArrayLookup n) {
        visit(n.e1);
        visit(n.e2);
        
        // Marcar la variable del array como usada
        if (n.e1 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e1).s);
            if (var != null) {
                variablesUsadas.put(((IdentifierExpr) n.e1).s, true);
            }
        }
    }

    /**
     * Visita una operación de longitud de array.
     * Recorre la expresión del array.
     */
    public void visit(ArrayLength n) {
        visit(n.e);
        
        // Marcar la variable del array como usada
        if (n.e instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e).s);
            if (var != null) {
                variablesUsadas.put(((IdentifierExpr) n.e).s, true);
            }
        }
    }

    /**
     * Visita una llamada a método.
     * Recorre el objeto y los argumentos.
     */
    public void visit(Call n) {
        visit(n.e);
        for (int i = 0; i < n.el.size(); i++) {
            visit(n.el.get(i));
        }
    }

    /**
     * Visita un literal entero.
     */
    public void visit(IntegerLiteral n) {
        // No se requiere acción
    }

    /**
     * Visita una expresión de identificador.
     * Marca la variable como usada.
     */
    public void visit(IdentifierExpr n) {
        Variable var = scopeStack.lookup(n.s);
        if (var != null) {
            variablesUsadas.put(n.s, true);
        }
    }

    /**
     * Visita una referencia this.
     */
    public void visit(This n) {
        // No se requiere acción
    }

    /**
     * Visita una creación de array.
     * Recorre la expresión del tamaño.
     */
    public void visit(NewArray n) {
        visit(n.e);
    }

    /**
     * Visita una creación de objeto.
     */
    public void visit(NewObject n) {
        // No se requiere acción
    }

    /**
     * Visita un identificador.
     */
    public void visit(Identifier n) {
        // No se requiere acción
    }

    /**
     * Visita un parámetro.
     */
    public void visit(Param n) {
        // No se requiere acción
    }

    /**
     * Visita un tipo de array de enteros.
     */
    public void visit(IntArrayType n) {
        // No se requiere acción
    }

    /**
     * Visita un tipo entero.
     */
    public void visit(IntType n) {
        // No se requiere acción
    }

    /**
     * Visita un tipo de clase.
     */
    public void visit(ClassType n) {
        // No se requiere acción
    }
} 