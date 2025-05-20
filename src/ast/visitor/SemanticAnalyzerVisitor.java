package ast.visitor;

import ast.*;
import java.util.*;

/**
 * Visitante que realiza el análisis semántico del código fuente.
 * Verifica tipos, variables no utilizadas, y otras reglas semánticas.
 */
public class SemanticAnalyzerVisitor implements Visitor {
    // Pila de ámbitos para manejar el scope de variables
    private VariableScopeStack scopeStack;
    // Lista de errores semánticos encontrados
    private List<SemanticError> errors;
    // Tabla de clases para verificar herencia y tipos
    private Map<String, ClassDecl> classTable;
    // Clase actual siendo analizada
    private String currentClass;
    // Método actual siendo analizado
    private String currentMethod;
    // Tipo de retorno del método actual
    private Type currentMethodReturnType;
    // Parámetros del método actual
    private List<Type> currentMethodParams;
    // Indica si estamos en la clase main
    private boolean inMainClass;
    private Goal currentGoal;

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

    /**
     * Retorna la lista de errores semánticos encontrados.
     * Antes de retornar, verifica variables no utilizadas.
     */
    public List<SemanticError> getErrors() {
        checkUnusedVariables();
        return errors;
    }

    /**
     * Verifica variables no utilizadas en el ámbito actual.
     * Agrega errores para cada variable declarada pero no usada.
     */
    private void checkUnusedVariables() {
        VariableScope currentScope = scopeStack.getCurrentScope();
        if (currentScope != null) {
            Map<String, Variable> variables = currentScope.getVariables();
            for (Map.Entry<String, Variable> entry : variables.entrySet()) {
                Variable var = entry.getValue();
                if (!var.used) {
                    addError("Variable '" + var.id.s + "' declarada pero nunca utilizada", var.id.line);
                }
            }
        }
    }

    /**
     * Agrega un error semántico a la lista de errores.
     */
    private void addError(String message, int lineNumber) {
        errors.add(new SemanticError(message, lineNumber));
    }

    /**
     * Verifica si un tipo es subtipo de otro.
     * Maneja herencia de clases y tipos básicos.
     */
    private boolean isSubtype(Type t1, Type t2) {
        if (t1 instanceof ClassType && t2 instanceof ClassType) {
            String className1 = ((ClassType) t1).className;
            String className2 = ((ClassType) t2).className;
            
            if (className1.equals(className2)) {
                return true;
            }
            
            // Verifica herencia
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
                classTable.put(cs.i.s, cs);
            } else if (c instanceof ClassDeclExtends) {
                ClassDeclExtends ce = (ClassDeclExtends) c;
                classTable.put(ce.i.s, ce);
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
        inMainClass = true;
        currentClass = n.i1.s;
        scopeStack.pushScope();
        
        // Agregar parámetro args
        ClassType stringArrayType = new ClassType(n.i2.line, "String[]");
        try {
            insertSymbol(stringArrayType, n.i2, null);
            // Marcar args como usado ya que es un parámetro requerido
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
        checkUnusedVariables();
        scopeStack.popScope();
        inMainClass = false;
        currentClass = null;
    }

    /**
     * Visita una clase simple (sin herencia).
     * Analiza sus variables y métodos.
     */
    public void visit(ClassDeclSimple n) {
        currentClass = n.i.s;
        scopeStack.pushScope();
        
        // Visitar variables y métodos
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.ml.size(); i++) {
            visit(n.ml.get(i));
        }
        
        checkUnusedVariables();
        scopeStack.popScope();
        currentClass = null;
    }

    /**
     * Visita una clase con herencia.
     * Analiza sus variables y métodos, considerando la herencia.
     */
    public void visit(ClassDeclExtends n) {
        currentClass = n.i.s;
        scopeStack.pushScope();
        
        // Visitar variables y métodos
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        for (int i = 0; i < n.ml.size(); i++) {
            visit(n.ml.get(i));
        }
        
        checkUnusedVariables();
        scopeStack.popScope();
        currentClass = null;
    }

    /**
     * Visita una declaración de variable simple.
     * Verifica que no esté duplicada en el ámbito actual.
     */
    public void visit(VarDeclSimple n) {
        try {
            insertSymbol(n.t, n.i, null);
        } catch (SemanticError e) {
            addError(e.getMessage(), e.getLineNumber());
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
                addError("Error de tipo en asignación de variable", n.i.line);
            }
        } catch (SemanticError e) {
            addError(e.getMessage(), e.getLineNumber());
        }
    }

    /**
     * Visita una declaración de método.
     * Analiza parámetros, variables locales y cuerpo del método.
     */
    public void visit(MethodDecl n) {
        currentMethod = n.i.s;
        currentMethodReturnType = n.t;
        currentMethodParams = new ArrayList<>();
        
        scopeStack.pushScope();
        
        // Agregar parámetros al ámbito
        for (int i = 0; i < n.fl.size(); i++) {
            Param p = n.fl.get(i);
            currentMethodParams.add(p.t);
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
        
        // Verificar tipo de retorno
        Type returnType = getExpressionType(n.e);
        if (!isSubtype(returnType, currentMethodReturnType)) {
            addError("Error de tipo en retorno del método " + currentMethod, n.e.line);
        }
        
        // Marcar parámetros como usados si se usan en el cuerpo
        for (int i = 0; i < n.fl.size(); i++) {
            Param p = n.fl.get(i);
            Variable var = scopeStack.lookup(p.i.s);
            if (var != null) {
                var.used = true;
            }
        }
        
        checkUnusedVariables();
        scopeStack.popScope();
        currentMethod = null;
        currentMethodReturnType = null;
        currentMethodParams = null;
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
            addError("La condición debe ser de tipo int", n.e.line);
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
            addError("La condición debe ser de tipo int", n.e.line);
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
            addError("La expresión a imprimir debe ser de tipo int", n.e.line);
        }
        // Marcar variables usadas en la expresión de impresión
        if (n.e instanceof IdentifierExpr) {
            Variable var = scopeStack.lookup(((IdentifierExpr) n.e).s);
            if (var != null) {
                var.used = true;
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
        if (!isSubtype(exprType, var.type)) {
            addError("Error de tipo en asignación", n.i.line);
        }
        
        // Marcar variables usadas en la expresión
        if (n.e instanceof IdentifierExpr) {
            Variable exprVar = scopeStack.lookup(((IdentifierExpr) n.e).s);
            if (exprVar != null) {
                exprVar.used = true;
            }
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
            addError("El índice del array debe ser de tipo int", n.e1.line);
        }
        
        Type valueType = getExpressionType(n.e2);
        if (!(valueType instanceof IntType)) {
            addError("El elemento del array debe ser de tipo int", n.e2.line);
        }
        
        // Marcar variables usadas en las expresiones
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
     * Visita una expresión de operador lógico.
     * Verifica que los operandos sean de tipo entero.
     */
    public void visit(And n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Los operandos de AND deben ser de tipo int", n.e1.line);
        }
    }

    public void visit(Or n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Los operandos de OR deben ser de tipo int", n.e1.line);
        }
    }

    public void visit(LessThan n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Los operandos de < deben ser de tipo int", n.e1.line);
        }
    }

    public void visit(MoreThan n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Los operandos de > deben ser de tipo int", n.e1.line);
        }
    }

    public void visit(Equal n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!isSubtype(t1, t2) && !isSubtype(t2, t1)) {
            addError("Los operandos de == deben ser de tipos compatibles", n.e1.line);
        }
    }

    public void visit(NotEqual n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!isSubtype(t1, t2) && !isSubtype(t2, t1)) {
            addError("Los operandos de != deben ser de tipos compatibles", n.e1.line);
        }
    }

    public void visit(Plus n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Los operandos de + deben ser de tipo int", n.e1.line);
        }
    }

    public void visit(Minus n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Los operandos de - deben ser de tipo int", n.e1.line);
        }
    }

    public void visit(Mult n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Los operandos de * deben ser de tipo int", n.e1.line);
        }
    }

    public void visit(Div n) {
        Type t1 = getExpressionType(n.e1);
        Type t2 = getExpressionType(n.e2);
        if (!(t1 instanceof IntType) || !(t2 instanceof IntType)) {
            addError("Los operandos de / deben ser de tipo int", n.e1.line);
        }
    }

    public void visit(ArrayLookup n) {
        Type arrayType = getExpressionType(n.e1);
        Type indexType = getExpressionType(n.e2);
        
        if (!(arrayType instanceof IntArrayType)) {
            addError("Se debe acceder a un array", n.e1.line);
        }
        if (!(indexType instanceof IntType)) {
            addError("El índice del array debe ser de tipo int", n.e2.line);
        }
        
        // Marcar variables usadas en el acceso al array
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
            addError("Solo se puede acceder a length de arrays", n.e.line);
        }
    }

    public void visit(Call n) {
        Type objType = getExpressionType(n.e);
        if (!(objType instanceof ClassType)) {
            addError("La llamada a método debe ser sobre un objeto", n.e.line);
            return;
        }
        
        String className = ((ClassType) objType).className;
        ClassDecl classDecl = classTable.get(className);
        if (classDecl == null) {
            addError("Clase " + className + " no encontrada", n.e.line);
            return;
        }
        
        // Buscar método en la jerarquía de clases
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
            addError("Método " + n.i.s + " no encontrado en clase " + className, n.i.line);
            return;
        }
        
        // Verificar número de argumentos
        if (n.el.size() != method.fl.size()) {
            addError("Número incorrecto de argumentos en llamada a método", n.i.line);
            return;
        }
        
        // Verificar tipos de argumentos
        for (int i = 0; i < n.el.size(); i++) {
            Type argType = getExpressionType(n.el.get(i));
            Type paramType = method.fl.get(i).t;
            if (!isSubtype(argType, paramType)) {
                addError("Error de tipo en argumento de llamada a método", n.el.get(i).line);
            }
        }
    }

    public void visit(IntegerLiteral n) {
        // No se requiere acción
    }

    public void visit(IdentifierExpr n) {
        Variable var = scopeStack.lookup(n.s);
        if (var == null) {
            addError("Variable " + n.s + " no declarada", n.line);
            return;
        }
        // Marcar variable como usada cuando aparece en una expresión
        var.used = true;
    }

    public void visit(This n) {
        if (currentClass == null) {
            addError("'this' no puede usarse en contexto estático", n.line);
        }
    }

    public void visit(NewArray n) {
        Type sizeType = getExpressionType(n.e);
        if (!(sizeType instanceof IntType)) {
            addError("El tamaño del array debe ser de tipo int", n.e.line);
        }
    }

    public void visit(NewObject n) {
        if (!classTable.containsKey(n.i.s)) {
            addError("Clase " + n.i.s + " no encontrada", n.i.line);
        }
    }

    public void visit(Identifier n) {
        // No se requiere acción
    }

    /**
     * Obtiene el tipo de una expresión.
     * Usado para verificación de tipos en tiempo de compilación.
     */
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
            // Encontrar tipo de retorno del método
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

    /**
     * Inserta un símbolo en el ámbito actual.
     * Usado para declarar variables y parámetros.
     */
    private void insertSymbol(Type t, Identifier id, Expr expOpt) throws SemanticError {
        scopeStack.insertSymbol(t, id, expOpt);
    }
} 