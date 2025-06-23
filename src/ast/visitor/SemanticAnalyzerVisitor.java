package ast.visitor;

import ast.*;
import java.util.*;

/**
 * Visitante que realiza el análisis semántico del código fuente.
 * Este visitante recorre el árbol sintáctico abstracto (AST) y verifica:
 * - Tipos de datos y compatibilidad entre tipos
 * - Variables no declaradas o duplicadas
 * - Variables no utilizadas
 * - Llamadas a métodos y acceso a miembros
 */
public class SemanticAnalyzerVisitor implements Visitor {
    // Pila de ámbitos para manejar el scope de variables
    private VariableScopeStack scopeStack;
    // Lista de errores semánticos encontrados durante el análisis
    private List<SemanticError> errores;
        private Map<String, ClassDecl> tablaDeClases;
    private String claseActual;
    private String metodoActual;
    private Type retornoTipoMetodoActual;
    private List<Type> parametrosMetodoActual;
    /**
     * Constructor del visitante de análisis semántico.
     * Inicializa todas las estructuras de datos necesarias.
     */
    public SemanticAnalyzerVisitor() {
        this.scopeStack = new VariableScopeStack();
        this.errores = new ArrayList<>();
        this.tablaDeClases = new HashMap<>();
        this.claseActual = null;
        this.metodoActual = null;
        this.retornoTipoMetodoActual = null;
        this.parametrosMetodoActual = new ArrayList<>();
    }

    /**
     * Retorna la lista de errores semánticos encontrados.
     * Antes de retornar, verifica variables no utilizadas.
     * Los errores se ordenan por número de línea.
     */
    public List<SemanticError> getErrores() {
        checkVariablesNoUsadas();
        // Ordenar errores por número de línea
        Collections.sort(errores, (e1, e2) -> Integer.compare(e1.getLineNumber(), e2.getLineNumber()));
        return new ArrayList<>(errores); // Retornar una copia para evitar modificaciones
    }

    /**
     * Verifica variables no utilizadas en el ámbito actual.
     * Solo muestra advertencias, nunca agrega errores.
     */
    private void checkVariablesNoUsadas() {
        VariableScope scopeActual = scopeStack.getCurrentScope();
        if (scopeActual != null) {
            Map<String, Variable> variables = scopeActual.getVariables();
            for (Map.Entry<String, Variable> entry : variables.entrySet()) {
                Variable var = entry.getValue();
                if (!var.used) {
                    // Solo advertencia, NO error
                    System.out.println("Advertencia: Variable '" + var.id.s + "' declarada pero nunca utilizada en una expresion (linea " + var.id.line + ")");
                }
            }
        }
    }

    /**
     * Agrega un error semántico a la lista de errores.
     * Formatea el mensaje de error con el número de línea.
     */
    private void addError(String message, int lineNumber) {
        // Eliminar cualquier prefijo existente de "Error en la linea"
        String cleanMessage = message.replaceAll("Error en la linea \\d+: ", "");
        // Verificar si el mensaje ya contiene el prefijo
        if (!cleanMessage.startsWith("Error en la linea")) {
            errores.add(new SemanticError("Error en la linea " + lineNumber + ": " + cleanMessage, lineNumber));
        } else {
            errores.add(new SemanticError(cleanMessage, lineNumber));
        }
    }

    /**
     * Verifica si un tipo es subtipo de otro.
     * Maneja herencia de clases y tipos básicos.
     * @param t1 Tipo a verificar
     * @param t2 Tipo base
     * @return true si t1 es subtipo de t2, false en caso contrario
     */
    private boolean isSubtype(Type t1, Type t2) {
        if (t1 == null || t2 == null) {
            return false;
        }

        // Si son el mismo tipo, son compatibles
        if (t1.getClass().equals(t2.getClass())) {
            if (t1 instanceof ClassType && t2 instanceof ClassType) {
                return ((ClassType) t1).className.equals(((ClassType) t2).className);
            }
            return true;
        }

        // Verificar herencia para tipos de clase
        if (t1 instanceof ClassType && t2 instanceof ClassType) {
            String className1 = ((ClassType) t1).className;
            String className2 = ((ClassType) t2).className;
            
            if (className1.equals(className2)) {
                return true;
            }
            
            Set<String> visited = new HashSet<>();
            ClassDecl classDecl = tablaDeClases.get(className1);
            while (classDecl instanceof ClassDeclExtends) {
                String parentName = ((ClassDeclExtends) classDecl).j.s;
                if (parentName.equals(className2)) {
                    return true;
                }
                if (!visited.add(parentName)) {
                    return false;
                }
                classDecl = tablaDeClases.get(parentName);
            }
        }

        // No hay compatibilidad entre tipos diferentes
        return false;
    }

    /**
     * Visita el nodo Goal (programa completo).
     * Realiza dos pasadas:
     * 1. Recolecta todas las declaraciones de clases
     * 2. Analiza el código
     */
    public void visit(Goal n) {
        // Primera pasada: recolectar declaraciones de clases
        for (int i = 0; i < n.cl.size(); i++) {
            ClassDecl c = n.cl.get(i);
            if (c instanceof ClassDeclSimple) {
                ClassDeclSimple cs = (ClassDeclSimple) c;
                tablaDeClases.put(cs.i.s, cs);
            } else if (c instanceof ClassDeclExtends) {
                ClassDeclExtends ce = (ClassDeclExtends) c;
                tablaDeClases.put(ce.i.s, ce);
            }
        }

        // Segunda pasada: analizar
        visit(n.m);
        for (int i = 0; i < n.cl.size(); i++) {
            visit(n.cl.get(i));
        }
    }

    /**
     * Visita una declaración de clase.
     * Redirige al visitante específico según el tipo de clase.
     */
    public void visit(ClassDecl c) {
        if (c instanceof ClassDeclExtends) {
            visit((ClassDeclExtends) c);
        } else if (c instanceof ClassDeclSimple) {
            visit((ClassDeclSimple) c);
        }
    }

    /**
     * Visita la clase main.
     * Maneja el ámbito especial de la clase main y sus variables.
     */
    public void visit(MainClass n) {
        claseActual = n.i1.s;
        scopeStack.pushScope();
        
        // Agregar parametro args
        ClassType stringArrayType = new ClassType(n.i2.line, "String[]");
        try {
            insertSymbol(stringArrayType, n.i2, null);
            // Marcar args como usado ya que es un parametro requerido
            Variable argsVar = scopeStack.lookup(n.i2.s);
            if (argsVar != null) {
                argsVar.used = true;
            }
        } catch (SemanticError e) {
            addError(e.getMessage(), e.getLineNumber());
        }
        
        // Visitar variables y sentencias
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
        }
        checkVariablesNoUsadas();
        scopeStack.popScope();
        claseActual = null;
    }

    /**
     * Visita una clase simple (sin herencia).
     * Analiza sus variables y métodos.
     */
    public void visit(ClassDeclSimple n) {
        claseActual = n.i.s;
        scopeStack.pushScope();
        
        // Visitar variables y métodos
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.ml.size(); i++) {
            visit(n.ml.get(i));
        }
        
        checkVariablesNoUsadas();
        scopeStack.popScope();
        claseActual = null;
    }

    /**
     * Visita una clase con herencia.
     * Analiza sus variables y métodos, considerando la herencia.
     */
    public void visit(ClassDeclExtends n) {
        claseActual = n.i.s;
        scopeStack.pushScope();
        
        // Verificar que la clase padre exista
        if (!tablaDeClases.containsKey(n.j.s)) {
            addError("Clase padre '" + n.j.s + "' no encontrada", n.j.line);
        }
        
        // Visitar variables y métodos
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.ml.size(); i++) {
            visit(n.ml.get(i));
        }
        
        checkVariablesNoUsadas();
        scopeStack.popScope();
        claseActual = null;
    }

    /**
     * Visita una declaración de variable simple.
     * Verifica que no esté duplicada en el ámbito actual.
     */
    public void visit(VarDeclSimple n) {
        try {
            insertSymbol(n.t, n.i, null);
        } catch (SemanticError e) {
            addError(e.getMessage().replace("Error en la linea " + e.getLineNumber() + ": ", ""), e.getLineNumber());
        }
    }

    /**
     * Visita una declaración de variable con asignación.
     * Verifica tipos y que no esté duplicada.
     */
    public void visit(VarDeclAssign n) {
        try {
            insertSymbol(n.t, n.i, n.e);
            Type exprType = getExpressionType(n.e);
            if (!isSubtype(exprType, n.t)) {
                addError("Error de tipo en asignacion de variable", n.i.line);
            }
        } catch (SemanticError e) {
            addError(e.getMessage().replace("Error en la linea " + e.getLineNumber() + ": ", ""), e.getLineNumber());
        }
    }

    /**
     * Visita una declaración de método.
     * Analiza parámetros, variables locales y cuerpo del método.
     */
    public void visit(MethodDecl n) {
        metodoActual = n.i.s;
        retornoTipoMetodoActual = n.t;
        parametrosMetodoActual = new ArrayList<>();
        
        scopeStack.pushScope();
        
        // Agregar parámetros al ámbito
        for (int i = 0; i < n.fl.size(); i++) {
            Param p = n.fl.get(i);
            parametrosMetodoActual.add(p.t);
            try {
                insertSymbol(p.t, p.i, null);
            } catch (SemanticError e) {
                addError(e.getMessage(), e.getLineNumber());
            }
        }
        
        // Visitar variables y sentencias
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
        }
        
        // Verificar tipo de retorno y marcar variables usadas en el retorno
        Type returnType = getExpressionType(n.e);
        if (n.e instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e).s);
            if (var != null) {
                var.used = true;
            }
        }
        if (!isSubtype(returnType, retornoTipoMetodoActual)) {
            addError("Error de tipo en retorno del método " + metodoActual + 
                    ": no se puede retornar " + getTypeName(returnType) + 
                    " donde se espera " + getTypeName(retornoTipoMetodoActual), n.e.line);
        }
        
        // Marcar parámetros como usados si se usan en el cuerpo
        for (int i = 0; i < n.fl.size(); i++) {
            Param p = n.fl.get(i);
            Variable var = scopeStack.lookup(p.i.s);
            if (var != null) {
                var.used = true;
            }
        }
        
        checkVariablesNoUsadas();
        scopeStack.popScope();
        metodoActual = null;
        retornoTipoMetodoActual = null;
        parametrosMetodoActual = null;
    }

    /**
     * Visita un parámetro de método.
     * La lógica principal está en MethodDecl.
     */
    public void visit(Param n) {
        // Manejado en MethodDecl
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
     * Verifica que la condición sea de tipo entero.
     */
    public void visit(If n) {
        Type condType = getExpressionType(n.e);
        if (!(condType instanceof IntType)) {
            addError("La condicion debe ser de tipo int", n.e.line);
        }
        
        // Marcar variables usadas en la condición
        if (n.e instanceof LessThan) {
            LessThan lt = (LessThan) n.e;
            if (lt.e1 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) lt.e1).s);
                if (var != null) var.used = true;
            }
            if (lt.e2 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) lt.e2).s);
                if (var != null) var.used = true;
            }
        } else if (n.e instanceof MoreThan) {
            MoreThan mt = (MoreThan) n.e;
            if (mt.e1 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) mt.e1).s);
                if (var != null) var.used = true;
            }
            if (mt.e2 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) mt.e2).s);
                if (var != null) var.used = true;
            }
        } else if (n.e instanceof Equal) {
            Equal eq = (Equal) n.e;
            if (eq.e1 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) eq.e1).s);
                if (var != null) var.used = true;
            }
            if (eq.e2 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) eq.e2).s);
                if (var != null) var.used = true;
            }
        } else if (n.e instanceof Or) {
            Or or = (Or) n.e;
            if (or.e1 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) or.e1).s);
                if (var != null) var.used = true;
            }
            if (or.e2 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) or.e2).s);
                if (var != null) var.used = true;
            }
        } else if (n.e instanceof And) {
            And and = (And) n.e;
            if (and.e1 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) and.e1).s);
                if (var != null) var.used = true;
            }
            if (and.e2 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) and.e2).s);
                if (var != null) var.used = true;
            }
        }
        
        visit(n.s1);
        visit(n.s2);
    }

    /**
     * Visita una sentencia while.
     * Verifica que la condición sea de tipo entero.
     */
    public void visit(While n) {
        Type condType = getExpressionType(n.e);
        if (!(condType instanceof IntType)) {
            addError("La condicion debe ser de tipo int", n.e.line);
        }
        
        // Marcar variables usadas en la condición
        if (n.e instanceof LessThan) {
            LessThan lt = (LessThan) n.e;
            if (lt.e1 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) lt.e1).s);
                if (var != null) var.used = true;
            }
            if (lt.e2 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) lt.e2).s);
                if (var != null) var.used = true;
            }
        } else if (n.e instanceof MoreThan) {
            MoreThan mt = (MoreThan) n.e;
            if (mt.e1 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) mt.e1).s);
                if (var != null) var.used = true;
            }
            if (mt.e2 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) mt.e2).s);
                if (var != null) var.used = true;
            }
        } else if (n.e instanceof Equal) {
            Equal eq = (Equal) n.e;
            if (eq.e1 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) eq.e1).s);
                if (var != null) var.used = true;
            }
            if (eq.e2 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) eq.e2).s);
                if (var != null) var.used = true;
            }
        } else if (n.e instanceof Or) {
            Or or = (Or) n.e;
            if (or.e1 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) or.e1).s);
                if (var != null) var.used = true;
            }
            if (or.e2 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) or.e2).s);
                if (var != null) var.used = true;
            }
        } else if (n.e instanceof And) {
            And and = (And) n.e;
            if (and.e1 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) and.e1).s);
                if (var != null) var.used = true;
            }
            if (and.e2 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) and.e2).s);
                if (var != null) var.used = true;
            }
        }
        
        visit(n.s);
    }

    /**
     * Visita una sentencia print.
     * Verifica que la expresión sea de tipo entero.
     */
    public void visit(Print n) {
        Type exprType = getExpressionType(n.e);
        if (!(exprType instanceof IntType)) {
            addError("La expresion a imprimir debe ser de tipo int", n.e.line);
        }
        
        // Marcar variables usadas en la expresion
        if (n.e instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e).s);
            if (var != null) {
                var.used = true;
            }
        } else if (n.e instanceof ArrayLookup) {
            ArrayLookup al = (ArrayLookup) n.e;
            if (al.e1 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) al.e1).s);
                if (var != null) {
                    var.used = true;
                }
            }
            if (al.e2 instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) al.e2).s);
                if (var != null) {
                    var.used = true;
                }
            }
        }
    }

    /**
     * Visita una asignación.
     * Verifica que la variable exista y los tipos coincidan.
     */
    public void visit(Assign n) {
        Variable var = scopeStack.lookup(n.i.s);
        if (var == null) {
            addError("Variable " + n.i.s + " no declarada", n.i.line);
            return;
        }
        
        Type exprType = getExpressionType(n.e);
        
        if (n.e instanceof IdentifierExpr) {
            Variable exprVar = scopeStack.lookup(((IdentifierExpr) n.e).s);
            if (exprVar != null) {
                exprVar.used = true;
            } else {
                addError("Variable '" + ((IdentifierExpr) n.e).s + "' no declarada", n.e.line);
                return;
            }
        }
        
        if (exprType == null) {
            if (n.e instanceof IdentifierExpr) {
                addError("Variable '" + ((IdentifierExpr) n.e).s + "' no declarada", n.e.line);
            } else {
                addError("Error de tipo en asignacion: expresion no valida", n.i.line);
            }
            return;
        }
        
        if (!isSubtype(exprType, var.type)) {
            addError("Error de tipo en asignacion: no se puede asignar " + 
                    getTypeName(exprType) + " a " + getTypeName(var.type), n.i.line);
        }
    }

    /**
     * Visita una asignación a array.
     * Verifica tipos y que la variable sea un array.
     */
    public void visit(ArrayAssign n) {
        Variable var = scopeStack.lookup(n.i.s);
        if (var == null) {
            addError("Variable " + n.i.s + " no declarada", n.i.line);
            return;
        }
        
        if (!(var.type instanceof IntArrayType)) {
            addError("Variable " + n.i.s + " no es un array", n.i.line);
            return;
        }
        
        Type indexType = getExpressionType(n.e1);
        if (!(indexType instanceof IntType)) {
            addError("El indice del array debe ser de tipo int", n.e1.line);
        }
        
        Type valueType = getExpressionType(n.e2);
        if (!(valueType instanceof IntType)) {
            addError("El elemento del array debe ser de tipo int", n.e2.line);
        }
        
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

    /**
     * Visita una expresión de operador lógico AND.
     * Verifica que los operandos sean de tipo entero.
     */
    public void visit(And n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        
        if (t1 == null || t2 == null) {
            addError("Error de tipo en operacion &&: tipos incompatibles", n.e1.line);
            return;
        }
        
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Error de tipo en operacion &&: no se puede operar " + 
                    getTypeName(t1) + " con " + getTypeName(t2), n.e1.line);
        }
        
        // Marcar variables usadas en ambas expresiones
        if (n.e1 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e1).s);
            if (var != null) var.used = true;
        } else if (n.e1 instanceof Or || n.e1 instanceof And) {
            visit(n.e1);
        } else if (n.e1 instanceof LessThan || n.e1 instanceof MoreThan || n.e1 instanceof Equal) {
            visit(n.e1);
        }
        
        if (n.e2 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e2).s);
            if (var != null) var.used = true;
        } else if (n.e2 instanceof Or || n.e2 instanceof And) {
            visit(n.e2);
        } else if (n.e2 instanceof LessThan || n.e2 instanceof MoreThan || n.e2 instanceof Equal) {
            visit(n.e2);
        }
    }

    /**
     * Visita una expresión de operador lógico OR.
     * Verifica que los operandos sean de tipo entero.
     */
    public void visit(Or n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        
        if (t1 == null || t2 == null) {
            addError("Error de tipo en operacion ||: tipos incompatibles", n.e1.line);
            return;
        }
        
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Error de tipo en operacion ||: no se puede operar " + 
                    getTypeName(t1) + " con " + getTypeName(t2), n.e1.line);
        }
        
        // Marcar variables usadas en ambas expresiones
        if (n.e1 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e1).s);
            if (var != null) var.used = true;
        } else if (n.e1 instanceof Or || n.e1 instanceof And) {
            visit(n.e1);
        } else if (n.e1 instanceof LessThan || n.e1 instanceof MoreThan || n.e1 instanceof Equal) {
            visit(n.e1);
        }
        
        if (n.e2 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e2).s);
            if (var != null) var.used = true;
        } else if (n.e2 instanceof Or || n.e2 instanceof And) {
            visit(n.e2);
        } else if (n.e2 instanceof LessThan || n.e2 instanceof MoreThan || n.e2 instanceof Equal) {
            visit(n.e2);
        }
    }

    /**
     * Visita una comparación menor que.
     * Verifica que los operandos sean de tipo entero.
     */
    public void visit(LessThan n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        
        if (t1 == null || t2 == null) {
            addError("Error de tipo en operacion <: tipos incompatibles", n.e1.line);
            return;
        }
        
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Error de tipo en operacion <: no se puede comparar " + 
                    getTypeName(t1) + " con " + getTypeName(t2), n.e1.line);
        }
        
        if (n.e1 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e1).s);
            if (var != null) var.used = true;
        }
        if (n.e2 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e2).s);
            if (var != null) var.used = true;
        }
    }

    /**
     * Visita una comparación mayor que.
     * Verifica que los operandos sean de tipo entero.
     */
    public void visit(MoreThan n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        
        if (t1 == null || t2 == null) {
            addError("Error de tipo en operacion >: tipos incompatibles", n.e1.line);
            return;
        }
        
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Error de tipo en operacion >: no se puede comparar " + 
                    getTypeName(t1) + " con " + getTypeName(t2), n.e1.line);
        }
        
        if (n.e1 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e1).s);
            if (var != null) var.used = true;
        }
        if (n.e2 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e2).s);
            if (var != null) var.used = true;
        }
    }

    /**
     * Visita una comparación de igualdad.
     * Verifica que los operandos sean compatibles.
     */
    public void visit(Equal n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        
        if (t1 == null || t2 == null) {
            addError("Error de tipo en operacion ==: tipos incompatibles", n.e1.line);
            return;
        }
        
        if (!isSubtype(t1, t2) && !isSubtype(t2, t1)) {
            addError("Error de tipo en operacion ==: no se puede comparar " + 
                    getTypeName(t1) + " con " + getTypeName(t2), n.e1.line);
        }
        
        if (n.e1 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e1).s);
            if (var != null) var.used = true;
        }
        if (n.e2 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e2).s);
            if (var != null) var.used = true;
        }
    }

    /**
     * Visita una comparación de desigualdad.
     * Verifica que los operandos sean compatibles.
     */
    public void visit(NotEqual n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        
        if (t1 == null || t2 == null) {
            addError("Error de tipo en operacion !=: tipos incompatibles", n.e1.line);
            return;
        }
        
        if (!isSubtype(t1, t2) && !isSubtype(t2, t1)) {
            addError("Error de tipo en operacion !=: no se puede comparar " + 
                    getTypeName(t1) + " con " + getTypeName(t2), n.e1.line);
        }
        
        if (n.e1 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e1).s);
            if (var != null) var.used = true;
        }
        if (n.e2 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e2).s);
            if (var != null) var.used = true;
        }
    }

    /**
     * Visita una suma.
     * Verifica que los operandos sean de tipo entero.
     */
    public void visit(Plus n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        
        if (n.e1 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e1).s);
            if (var != null) var.used = true;
        }
        if (n.e2 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e2).s);
            if (var != null) var.used = true;
        }
        
        if (t1 == null || t2 == null) {
            if (t1 == null && n.e1 instanceof IdentifierExpr) {
                addError("Variable '" + ((IdentifierExpr) n.e1).s + "' no declarada", n.e1.line);
            }
            if (t2 == null && n.e2 instanceof IdentifierExpr) {
                addError("Variable '" + ((IdentifierExpr) n.e2).s + "' no declarada", n.e2.line);
            }
            return;
        }
        
        // Verificar que ambos operandos sean de tipo int
        if (!(t1 instanceof IntType)) {
            addError("Error de tipo en operacion +: el primer operando debe ser de tipo int, pero es " + 
                    getTypeName(t1), n.e1.line);
            return;
        }
        
        if (!(t2 instanceof IntType)) {
            addError("Error de tipo en operacion +: el segundo operando debe ser de tipo int, pero es " + 
                    getTypeName(t2), n.e2.line);
            return;
        }
    }

    /**
     * Visita una resta.
     * Verifica que los operandos sean de tipo entero.
     */
    public void visit(Minus n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        
        if (t1 == null || t2 == null) {
            addError("Error de tipo en operacion -: tipos incompatibles", n.e1.line);
            return;
        }
        
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Error de tipo en operacion -: no se puede restar " + 
                    getTypeName(t1) + " con " + getTypeName(t2), n.e1.line);
        }
        
        if (n.e1 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e1).s);
            if (var != null) var.used = true;
        }
        if (n.e2 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e2).s);
            if (var != null) var.used = true;
        }
    }

    /**
     * Visita una multiplicación.
     * Verifica que los operandos sean de tipo entero.
     */
    public void visit(Mult n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        
        if (t1 == null || t2 == null) {
            addError("Error de tipo en operacion *: tipos incompatibles", n.e1.line);
            return;
        }
        
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Error de tipo en operacion *: no se puede multiplicar " + 
                    getTypeName(t1) + " con " + getTypeName(t2), n.e1.line);
        }
        
        if (n.e1 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e1).s);
            if (var != null) var.used = true;
        }
        if (n.e2 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e2).s);
            if (var != null) var.used = true;
        }
    }

    /**
     * Visita una división.
     * Verifica que los operandos sean de tipo entero.
     */
    public void visit(Div n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        
        if (t1 == null || t2 == null) {
            addError("Error de tipo en operacion /: tipos incompatibles", n.e1.line);
            return;
        }
        
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Error de tipo en operacion /: no se puede dividir " + 
                    getTypeName(t1) + " con " + getTypeName(t2), n.e1.line);
            return;
        }

        // Verificar división por cero
        if (n.e2 instanceof IntegerLiteral) {
            IntegerLiteral divisor = (IntegerLiteral) n.e2;
            if (divisor.i == 0) {
                addError("Error: Division por cero no permitida", n.e2.line);
            }
        }
        
        if (n.e1 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e1).s);
            if (var != null) var.used = true;
        }
        if (n.e2 instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e2).s);
            if (var != null) var.used = true;
        }
    }

    /**
     * Visita un acceso a array.
     * Verifica que la variable sea un array y el índice sea entero.
     */
    public void visit(ArrayLookup n) {
        Type arrayType = getExpressionType(n.e1);
        Type indexType = getExpressionType(n.e2);
        
        if (!(arrayType instanceof IntArrayType)) {
            addError("Se debe acceder a un array", n.e1.line);
        }
        if (!(indexType instanceof IntType)) {
            addError("El indice del array debe ser de tipo int", n.e2.line);
        }
        
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

    /**
     * Visita una operación de longitud de array.
     * Verifica que la variable sea un array.
     */
    public void visit(ArrayLength n) {
        Type arrayType = getExpressionType(n.e);
        if (!(arrayType instanceof IntArrayType)) {
            addError("Solo se puede acceder a length de arrays", n.e.line);
        }
    }

    /**
     * Busca un método en una clase específica.
     * @param classDecl La clase donde buscar el método
     * @param methodName El nombre del método a buscar
     * @return El método encontrado o null si no existe
     */
    private MethodDecl findMethodInClass(ClassDecl classDecl, String methodName) {
        MethodDeclList methodList = null;
        if (classDecl instanceof ClassDeclSimple) {
            methodList = ((ClassDeclSimple) classDecl).ml;
        } else if (classDecl instanceof ClassDeclExtends) {
            methodList = ((ClassDeclExtends) classDecl).ml;
        }

        if (methodList != null) {
            for (int i = 0; i < methodList.size(); i++) {
                MethodDecl m = methodList.get(i);
                if (m.i.s.equals(methodName)) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * Visita una llamada a método.
     * Verifica que el método exista y los tipos de argumentos sean compatibles.
     */
    public void visit(Call n) {
        Type tipoObjeto = getExpressionType(n.e);
        
        if (n.e instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e).s);
            if (var != null) var.used = true;
        }
        
        if (!(tipoObjeto instanceof ClassType)) {
            addError("La llamada a metodo debe ser sobre un objeto", n.e.line);
            return;
        }

        String nombreClase = ((ClassType) tipoObjeto).className;
        ClassDecl declaracionClase = tablaDeClases.get(nombreClase);

        if (declaracionClase == null) {
            addError("Clase " + nombreClase + " no encontrada", n.e.line);
            return;
        }

        Set<String> clasesVisitadas = new HashSet<>();
        MethodDecl metodo = null;
        String nombreClaseActual = nombreClase;
        StringBuilder rutaHerencia = new StringBuilder(nombreClase);

        while (declaracionClase != null && !clasesVisitadas.contains(nombreClaseActual)) {
            clasesVisitadas.add(nombreClaseActual);
            metodo = findMethodInClass(declaracionClase, n.i.s);
            if (metodo != null) break;

            if (declaracionClase instanceof ClassDeclExtends) {
                nombreClaseActual = ((ClassDeclExtends) declaracionClase).j.s;
                declaracionClase = tablaDeClases.get(nombreClaseActual);
                if (declaracionClase != null) {
                    rutaHerencia.append(" -> ").append(nombreClaseActual);
                }
            } else {
                declaracionClase = null;
            }
        }

        if (metodo == null) {
            if (clasesVisitadas.size() > 1) {
                addError("Metodo '" + n.i.s + "' no existe en la jerarquia: " + rutaHerencia.toString(), n.i.line);
            } else {
                addError("Metodo '" + n.i.s + "' no existe en clase " + nombreClase, n.i.line);
            }
            return;
        }

        // Marcar variables usadas en los argumentos
        for (int i = 0; i < n.el.size(); i++) {
            Expr arg = n.el.get(i);
            if (arg instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) arg).s);
                if (var != null) {
                    var.used = true;
                }
            }
        }
    }

    /**
     * Obtiene un nombre legible para un tipo.
     */
    private String getTypeName(Type t) {
        if (t == null) return "null";
        if (t instanceof IntType) return "int";
        if (t instanceof IntArrayType) return "int[]";
        if (t instanceof ClassType) return ((ClassType) t).className;
        return t.getClass().getSimpleName();
    }

    /**
     * Visita un literal entero.
     */
    public void visit(IntegerLiteral n) {
        // No se requiere acción
    }

    /**
     * Visita una expresión de identificador.
     * Verifica que la variable exista y la marca como usada.
     */
    public void visit(IdentifierExpr n) {
        Variable var = scopeStack.lookup(n.s);
        if (var == null) {
            addError("Variable " + n.s + " no declarada", n.line);
            return;
        }
        var.used = true;
    }

    /**
     * Visita una referencia this.
     * Verifica que se use dentro de una clase.
     */
    public void visit(This n) {
        if (claseActual == null) {
            addError("'this' no puede usarse en contexto estático", n.line);
        }
    }

    /**
     * Visita una creación de array.
     * Verifica que el tamaño sea de tipo entero.
     */
    public void visit(NewArray n) {
        Type sizeType = getExpressionType(n.e);
        if (!(sizeType instanceof IntType)) {
            addError("El tamano del array debe ser de tipo int", n.e.line);
        }
    }

    /**
     * Visita una creación de objeto.
     * Verifica que la clase exista.
     */
    public void visit(NewObject n) {
        if (!tablaDeClases.containsKey(n.i.s)) {
            addError("Clase " + n.i.s + " no encontrada", n.i.line);
        }
    }

    /**
     * Visita un identificador.
     */
    public void visit(Identifier n) {
        // No se requiere acción
    }

    /**
     * Obtiene el tipo de una expresión.
     * Usado para verificación de tipos en tiempo de compilación.
     */
    private Type getExpressionType(Expr e) {
        if (e == null) return null;
        
        Type resultado = null;
        
        try {
            if (e instanceof IntegerLiteral) {
                resultado = new IntType(e.line);
            } else if (e instanceof IdentifierExpr) {
                Variable var = scopeStack.lookup(((IdentifierExpr) e).s);
                if (var != null) {
                    resultado = var.type;
                    var.used = true;
                } else {
                    addError("Variable '" + ((IdentifierExpr) e).s + "' no declarada", e.line);
                    return null;
                }
            } else if (e instanceof This) {
                if (claseActual != null) {
                    resultado = new ClassType(e.line, claseActual);
                }
            } else if (e instanceof NewArray) {
                resultado = new IntArrayType(e.line);
            } else if (e instanceof NewObject) {
                resultado = new ClassType(e.line, ((NewObject) e).i.s);
            } else if (e instanceof ArrayLength) {
                resultado = new IntType(e.line);
            } else if (e instanceof ArrayLookup) {
                ArrayLookup accesoArray = (ArrayLookup) e;
                Type tipoArray = getExpressionType(accesoArray.e1);
                Type tipoIndice = getExpressionType(accesoArray.e2);
                
                if (tipoArray instanceof IntArrayType && tipoIndice instanceof IntType) {
                    resultado = new IntType(e.line);
                }
                
                // Marcar la variable del array como usada
                if (accesoArray.e1 instanceof IdentifierExpr) {
                    Variable var = scopeStack.lookup(((IdentifierExpr) accesoArray.e1).s);
                    if (var != null) {
                        var.used = true;
                    }
                }
            } else if (e instanceof Call) {
                Call call = (Call) e;
                Type tipoObjeto = getExpressionType(call.e);
                if (tipoObjeto instanceof ClassType) {
                    String nombreClase = ((ClassType) tipoObjeto).className;
                    ClassDecl declaracionClase = tablaDeClases.get(nombreClase);
                    MethodDecl metodo = null;
                    
                    while (declaracionClase != null) {
                        metodo = findMethodInClass(declaracionClase, call.i.s);
                        if (metodo != null) {
                            resultado = metodo.t;
                            break;
                        }
                        if (declaracionClase instanceof ClassDeclExtends) {
                            declaracionClase = tablaDeClases.get(((ClassDeclExtends) declaracionClase).j.s);
                        } else {
                            declaracionClase = null;
                        }
                    }
                    
                    if (metodo == null) {
                        addError("Metodo '" + call.i.s + "' no existe en clase " + nombreClase, call.i.line);
                        return null;
                    }
                }
                
                // Marcar variables usadas en los argumentos
                for (int i = 0; i < call.el.size(); i++) {
                    Expr arg = call.el.get(i);
                    if (arg instanceof IdentifierExpr) {
                        Variable var = scopeStack.lookup(((IdentifierExpr) arg).s);
                        if (var != null) {
                            var.used = true;
                        }
                    }
                }
            } else if (e instanceof Plus) {
                Plus plus = (Plus) e;
                Type t1 = getExpressionType(plus.e1);
                Type t2 = getExpressionType(plus.e2);
                
                if (t1 == null || t2 == null) {
                    return null;
                }
                
                if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
                    addError("Error de tipo en operacion +: no se puede operar " + 
                            getTypeName(t1) + " con " + getTypeName(t2), e.line);
                    return null;
                }
                
                resultado = new IntType(e.line);
            } else if (e instanceof Minus) {
                Minus minus = (Minus) e;
                Type t1 = getExpressionType(minus.e1);
                Type t2 = getExpressionType(minus.e2);
                
                if (t1 == null || t2 == null) {
                    return null;
                }
                
                if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
                    addError("Error de tipo en operacion -: no se puede operar " + 
                            getTypeName(t1) + " con " + getTypeName(t2), e.line);
                    return null;
                }
                
                resultado = new IntType(e.line);
            } else if (e instanceof Mult) {
                Mult mult = (Mult) e;
                Type t1 = getExpressionType(mult.e1);
                Type t2 = getExpressionType(mult.e2);
                
                if (t1 == null || t2 == null) {
                    return null;
                }
                
                if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
                    addError("Error de tipo en operacion *: no se puede operar " + 
                            getTypeName(t1) + " con " + getTypeName(t2), e.line);
                    return null;
                }
                
                resultado = new IntType(e.line);
            } else if (e instanceof Div) {
                Div div = (Div) e;
                Type t1 = getExpressionType(div.e1);
                Type t2 = getExpressionType(div.e2);
                
                if (t1 == null || t2 == null) {
                    return null;
                }
                
                if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
                    addError("Error de tipo en operacion /: no se puede operar " + 
                            getTypeName(t1) + " con " + getTypeName(t2), e.line);
                    return null;
                }
                
                resultado = new IntType(e.line);
            } else if (e instanceof And) {
                And and = (And) e;
                Type t1 = getExpressionType(and.e1);
                Type t2 = getExpressionType(and.e2);
                
                if (t1 == null || t2 == null) {
                    return null;
                }
                
                if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
                    addError("Error de tipo en operacion &&: no se puede operar " + 
                            getTypeName(t1) + " con " + getTypeName(t2), e.line);
                    return null;
                }
                
                resultado = new IntType(e.line);
            } else if (e instanceof Or) {
                Or or = (Or) e;
                Type t1 = getExpressionType(or.e1);
                Type t2 = getExpressionType(or.e2);
                
                if (t1 == null || t2 == null) {
                    return null;
                }
                
                if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
                    addError("Error de tipo en operacion ||: no se puede operar " + 
                            getTypeName(t1) + " con " + getTypeName(t2), e.line);
                    return null;
                }
                
                resultado = new IntType(e.line);
            } else if (e instanceof LessThan) {
                LessThan lt = (LessThan) e;
                Type t1 = getExpressionType(lt.e1);
                Type t2 = getExpressionType(lt.e2);
                
                if (t1 == null || t2 == null) {
                    return null;
                }
                
                if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
                    addError("Error de tipo en operacion <: no se puede operar " + 
                            getTypeName(t1) + " con " + getTypeName(t2), e.line);
                    return null;
                }
                
                resultado = new IntType(e.line);
            } else if (e instanceof MoreThan) {
                MoreThan mt = (MoreThan) e;
                Type t1 = getExpressionType(mt.e1);
                Type t2 = getExpressionType(mt.e2);
                
                if (t1 == null || t2 == null) {
                    return null;
                }
                
                if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
                    addError("Error de tipo en operacion >: no se puede operar " + 
                            getTypeName(t1) + " con " + getTypeName(t2), e.line);
                    return null;
                }
                
                resultado = new IntType(e.line);
            } else if (e instanceof Equal) {
                Equal eq = (Equal) e;
                Type t1 = getExpressionType(eq.e1);
                Type t2 = getExpressionType(eq.e2);
                
                if (t1 == null || t2 == null) {
                    return null;
                }
                
                if (!isSubtype(t1, t2) && !isSubtype(t2, t1)) {
                    addError("Error de tipo en operacion ==: no se puede comparar " + 
                            getTypeName(t1) + " con " + getTypeName(t2), e.line);
                    return null;
                }
                
                resultado = new IntType(e.line);
            } else if (e instanceof NotEqual) {
                NotEqual ne = (NotEqual) e;
                Type t1 = getExpressionType(ne.e1);
                Type t2 = getExpressionType(ne.e2);
                
                if (t1 == null || t2 == null) {
                    return null;
                }
                
                if (!isSubtype(t1, t2) && !isSubtype(t2, t1)) {
                    addError("Error de tipo en operacion !=: no se puede comparar " + 
                            getTypeName(t1) + " con " + getTypeName(t2), e.line);
                    return null;
                }
                
                resultado = new IntType(e.line);
            }
        } catch (Exception ex) {
            return null;
        }
        
        return resultado;
    }

    /**
     * Inserta un símbolo en el ámbito actual.
     * Usado para declarar variables y parámetros.
     * @throws SemanticError si la variable ya está declarada
     */
    private void insertSymbol(Type t, Identifier id, Expr expOpt) throws SemanticError {
        if (scopeStack.lookup(id.s) != null) {
            throw new SemanticError("Variable '" + id.s + "' ya declarada en este ambito", id.line);
        }
        scopeStack.insertSymbol(t, id, expOpt);
    }

    /**
     * Realiza el análisis semántico y la optimización del AST.
     * @param goal El nodo raíz del AST
     * @return Lista de errores semánticos encontrados
     */
    public List<SemanticError> analyze(Goal goal) {
        // Primero realizar el análisis semántico
        visit(goal);
        
        // Obtener los errores una sola vez
        List<SemanticError> errores = new ArrayList<>(this.errores);
        Collections.sort(errores, (e1, e2) -> Integer.compare(e1.getLineNumber(), e2.getLineNumber()));
        
        // Mostrar los errores una sola vez
        if (!errores.isEmpty()) {
            System.out.println("\nErrores semanticos encontrados:");
            for (SemanticError error : errores) {
                System.out.println(error.getMessage());
            }
        } else {
            System.out.println("\nNo se encontraron errores semanticos.");
        }
        
        return errores;
    }
} 