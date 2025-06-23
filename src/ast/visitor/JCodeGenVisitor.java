package ast.visitor;

import ast.*;
import java.io.*;
import java.util.*;

/**
 * Visitor que genera código Jasmin a partir del AST optimizado de MiniJava.
 * Por cada clase, crea un archivo .j con el código Jasmin correspondiente.
 * Utiliza un mapeo de variables locales y parámetros a índices de locales para cada método.
 * Solo declara .field para variables de instancia (campos de la clase).
 * No genera código si hay errores semánticos.
 */
public class JCodeGenVisitor implements Visitor {
    private Map<String, PrintWriter> writersClass = new HashMap<>();
    private String nombreClaseActual = null;
    private PrintWriter writerActual = null;
    // Bandera para saber si estamos declarando campos de clase (true) o variables locales (false)
    private boolean enClase = false;
    private Map<String, Integer> indiceVarLocal = null;
    private Map<String, Type> tipoVarLocal = null;
    private int siguienteIndiceLocal = 0;
    private String claseMetodoActual = null;

    /**
     * Cierra todos los writers abiertos al finalizar la generación de código.
     */
    private void cerrarWriters() {
        for (PrintWriter writer : writersClass.values()) {
            writer.close();
        }
    }

    /**
     * Crea un PrintWriter para el archivo .j de una clase y lo guarda en el mapa.
     * @param nombreClase Nombre de la clase
     * @return PrintWriter para escribir el archivo .j
     */
    private PrintWriter crearWriterClass(String nombreClase) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(nombreClase + ".j"));
            writersClass.put(nombreClase, writer);
            return writer;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el archivo Jasmin para la clase: " + nombreClase, e);
        }
    }

    // ========== VISITADORES PRINCIPALES ==========

    /**
     * Visita el nodo raíz Goal (programa completo).
     * Genera el código para la clase principal y todas las clases del programa.
     */
    @Override
    public void visit(Goal n) {
        visit(n.m);
        for (int i = 0; i < n.cl.size(); i++) {
            visit(n.cl.get(i));
        }
        cerrarWriters();
    }

    /**
     * Visita la clase principal (MainClass).
     * Genera el archivo .j y el método main, inicializando la tabla de variables locales.
     */
    @Override
    public void visit(MainClass n) {
        String className = n.i1.s;
        nombreClaseActual = className;
        writerActual = crearWriterClass(className);
        PrintWriter writer = writerActual;
        // Encabezado Jasmin básico para la clase principal
        writer.println(".class public " + className);
        writer.println(".super java/lang/Object");
        writer.println();
        // Constructor por defecto
        writer.println(".method public <init>()V");
        writer.println("   aload_0");
        writer.println("   invokespecial java/lang/Object/<init>()V");
        writer.println("   return");
        writer.println(".end method");
        writer.println();
        // Método main
        writer.println(".method public static main([Ljava/lang/String;)V");
        // Inicializar tabla de variables locales y tipos para main
        indiceVarLocal = new HashMap<>();
        tipoVarLocal = new HashMap<>();
        siguienteIndiceLocal = 0;
        // args está en el local 0
        indiceVarLocal.put(n.i2.s, 0);
        tipoVarLocal.put(n.i2.s, new ClassType(n.i2.line, "String[]"));
        siguienteIndiceLocal = 1;
        // Variables locales declaradas en main
        for (int i = 0; i < n.vl.size(); i++) {
            VarDecl v = n.vl.get(i);
            String varName = (v instanceof VarDeclSimple) ? ((VarDeclSimple)v).i.s : ((VarDeclAssign)v).i.s;
            Type varType = (v instanceof VarDeclSimple) ? ((VarDeclSimple)v).t : ((VarDeclAssign)v).t;
            indiceVarLocal.put(varName, siguienteIndiceLocal);
            tipoVarLocal.put(varName, varType);
            siguienteIndiceLocal++;
        }
        int limitLocals = siguienteIndiceLocal;
        int limitStack = 8; // Estimación simple
        writer.println("   .limit stack " + limitStack);
        writer.println("   .limit locals " + limitLocals);
        // Sentencias del main
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
        }
        writer.println("   return");
        writer.println(".end method");
        writer.flush();
        // Limpiar tabla de variables locales
        indiceVarLocal = null;
        tipoVarLocal = null;
        siguienteIndiceLocal = 0;
    }

    /**
     * Visita una clase simple (sin herencia).
     * Genera el archivo .j, declara los campos y los métodos.
     */
    @Override
    public void visit(ClassDeclSimple n) {
        String className = n.i.s;
        nombreClaseActual = className;
        writerActual = crearWriterClass(className);
        PrintWriter writer = writerActual;
        writer.println(".class public " + className);
        writer.println(".super java/lang/Object");
        writer.println();
        // Constructor por defecto
        writer.println(".method public <init>()V");
        writer.println("   aload_0");
        writer.println("   invokespecial java/lang/Object/<init>()V");
        writer.println("   return");
        writer.println(".end method");
        writer.println();
        // Variables de instancia (campos)
        enClase = true;
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        enClase = false;
        // Métodos
        for (int i = 0; i < n.ml.size(); i++) {
            visit(n.ml.get(i));
        }
        writer.flush();
    }

    /**
     * Visita una clase con herencia.
     * Genera el archivo .j, declara los campos y los métodos, y el .super adecuado.
     */
    @Override
    public void visit(ClassDeclExtends n) {
        String className = n.i.s;
        String parentName = n.j.s;
        nombreClaseActual = className;
        writerActual = crearWriterClass(className);
        PrintWriter writer = writerActual;
        writer.println(".class public " + className);
        writer.println(".super " + parentName);
        writer.println();
        // Constructor por defecto
        writer.println(".method public <init>()V");
        writer.println("   aload_0");
        writer.println("   invokespecial " + parentName + "/<init>()V");
        writer.println("   return");
        writer.println(".end method");
        writer.println();
        // Variables de instancia (campos)
        enClase = true;
        for (int i = 0; i < n.vl.size(); i++) {
            visit(n.vl.get(i));
        }
        enClase = false;
        // Métodos
        for (int i = 0; i < n.ml.size(); i++) {
            visit(n.ml.get(i));
        }
        writer.flush();
    }

    /**
     * Declara un campo de clase (.field) solo si estamos en contexto de clase.
     * No genera nada para variables locales.
     */
    @Override
    public void visit(VarDeclSimple n) {
        if (enClase && writerActual != null) {
            String type = jasminType(n.t);
            writerActual.println(".field public " + n.i.s + " " + type);
        }
    }

    /**
     * Declara un campo de clase (.field) solo si estamos en contexto de clase.
     * No genera nada para variables locales.
     */
    @Override
    public void visit(VarDeclAssign n) {
        if (enClase && writerActual != null) {
            String type = jasminType(n.t);
            writerActual.println(".field public " + n.i.s + " " + type);
        }
    }

    /**
     * Genera el código Jasmin para un método.
     * Asigna índices a parámetros y variables locales, y genera el cuerpo y el return.
     */
    @Override
    public void visit(MethodDecl n) {
        PrintWriter writer = writerActual;
        String returnType = jasminType(n.t);
        StringBuilder params = new StringBuilder();
        // Inicializar tabla de variables locales y tipos
        indiceVarLocal = new HashMap<>();
        tipoVarLocal = new HashMap<>();
        siguienteIndiceLocal = 0;
        claseMetodoActual = nombreClaseActual;
        // Si el método NO es static, el primer local es 'this'
        boolean isStatic = false; // MiniJava no tiene métodos static salvo main
        if (n.i.s.equals("main")) isStatic = true;
        if (!isStatic) {
            indiceVarLocal.put("this", 0);
            tipoVarLocal.put("this", new ClassType(n.i.line, nombreClaseActual));
            siguienteIndiceLocal = 1;
        }
        // Parámetros
        for (int i = 0; i < n.fl.size(); i++) {
            Param p = n.fl.get(i);
            indiceVarLocal.put(p.i.s, siguienteIndiceLocal);
            tipoVarLocal.put(p.i.s, p.t);
            params.append(jasminType(p.t));
            siguienteIndiceLocal++;
        }
        // Variables locales
        for (int i = 0; i < n.vl.size(); i++) {
            VarDecl v = n.vl.get(i);
            String varName = (v instanceof VarDeclSimple) ? ((VarDeclSimple)v).i.s : ((VarDeclAssign)v).i.s;
            Type varType = (v instanceof VarDeclSimple) ? ((VarDeclSimple)v).t : ((VarDeclAssign)v).t;
            indiceVarLocal.put(varName, siguienteIndiceLocal);
            tipoVarLocal.put(varName, varType);
            siguienteIndiceLocal++;
        }
        // Calcular límites realistas(para no poner un stack demasiado grande)
        int limitLocals = siguienteIndiceLocal;
        int limitStack = 8; // Estimación simple
        writer.println();
        writer.println(".method public " + n.i.s + "(" + params + ")" + returnType);
        writer.println("   .limit stack " + limitStack);
        writer.println("   .limit locals " + limitLocals);
        // Sentencias
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
        }
        // Retorno: cargar el valor antes de return
        visit(n.e); // Esto deja el valor en el stack
        if (returnType.equals("I") || returnType.equals("[I")) {
            writer.println("   ireturn");
        } else {
            writer.println("   areturn");
        }
        writer.println(".end method");
        writer.flush();
        // Limpiar tabla de variables locales
        indiceVarLocal = null;
        tipoVarLocal = null;
        claseMetodoActual = null;
    }

    /**
     * Genera el código para una sentencia Print.
     * Llama a println con el tipo adecuado (int u objeto).
     */
    @Override
    public void visit(Print n) {
        PrintWriter writer = writerActual;
        writer.println("   getstatic java/lang/System/out Ljava/io/PrintStream;");
        visit(n.e);
        Type t = null;
        if (n.e instanceof IdentifierExpr) {
            String var = ((IdentifierExpr)n.e).s;
            t = tipoVarLocal != null ? tipoVarLocal.get(var) : null;
        }
        if (t == null || t instanceof IntType || t instanceof IntArrayType) {
            writer.println("   invokevirtual java/io/PrintStream/println(I)V");
        } else {
            writer.println("   invokevirtual java/io/PrintStream/println(Ljava/lang/Object;)V");
        }
    }

    /**
     * Genera el código para una asignación a variable local.
     * Usa istore/astore según el tipo y el índice correcto.
     */
    @Override
    public void visit(Assign n) {
        visit(n.e);
        PrintWriter writer = writerActual;
        if (indiceVarLocal == null || tipoVarLocal == null) {
            // No generar nada si estamos fuera de contexto válido
            return;
        }
        int idx = indiceVarLocal.getOrDefault(n.i.s, 1);
        Type t = tipoVarLocal.getOrDefault(n.i.s, new IntType(n.i.line));
        if (t instanceof IntType || t instanceof IntArrayType) {
            writer.println("   istore_" + idx);
        } else {
            writer.println("   astore_" + idx);
        }
    }

    /**
     * Genera el código para cargar una variable local en el stack.
     * Usa iload/aload según el tipo y el índice correcto.
     */
    @Override
    public void visit(IdentifierExpr n) {
        if (indiceVarLocal == null || tipoVarLocal == null) {
            // No generar nada si estamos fuera de contexto válido
            return;
        }
        int idx = indiceVarLocal.getOrDefault(n.s, 1);
        Type t = tipoVarLocal.getOrDefault(n.s, new IntType(n.line));
        if (t instanceof IntType || t instanceof IntArrayType) {
            writerActual.println("   iload_" + idx);
        } else {
            writerActual.println("   aload_" + idx);
        }
    }

    /**
     * Utilidad para obtener el tipo Jasmin a partir de un Type del AST.
     * @param t Tipo del AST
     * @return Descriptor Jasmin (I, [I, LClase;, V)
     */
    private String jasminType(Type t) {
        if (t instanceof IntType) return "I";
        if (t instanceof IntArrayType) return "[I";
        if (t instanceof ClassType) return "L" + ((ClassType) t).className + ";";
        return "V"; // void por defecto
    }

    @Override
    public void visit(And n) {
        // (a && b): si a es 0, salta al final con 0; si no, evalúa b
        String labelFalse = "LabelAndFalse" + n.hashCode();
        String labelEnd = "LabelAndEnd" + n.hashCode();
        visit(n.e1);
        writerActual.println("   ifeq " + labelFalse);
        visit(n.e2);
        writerActual.println("   ifeq " + labelFalse);
        writerActual.println("   ldc 1");
        writerActual.println("   goto " + labelEnd);
        writerActual.println(labelFalse + ":");
        writerActual.println("   ldc 0");
        writerActual.println(labelEnd + ":");
    }

    @Override
    public void visit(Or n) {
        // (a || b): si a es distinto de 0, salta al final con 1; si no, evalúa b
        String labelTrue = "LabelOrTrue" + n.hashCode();
        String labelEnd = "LabelOrEnd" + n.hashCode();
        visit(n.e1);
        writerActual.println("   ifne " + labelTrue);
        visit(n.e2);
        writerActual.println("   ifne " + labelTrue);
        writerActual.println("   ldc 0");
        writerActual.println("   goto " + labelEnd);
        writerActual.println(labelTrue + ":");
        writerActual.println("   ldc 1");
        writerActual.println(labelEnd + ":");
    }

    @Override
    public void visit(Equal n) {
        // (a == b)
        String labelTrue = "LabelEqTrue" + n.hashCode();
        String labelEnd = "LabelEqEnd" + n.hashCode();
        visit(n.e1);
        visit(n.e2);
        writerActual.println("   if_icmpeq " + labelTrue);
        writerActual.println("   ldc 0");
        writerActual.println("   goto " + labelEnd);
        writerActual.println(labelTrue + ":");
        writerActual.println("   ldc 1");
        writerActual.println(labelEnd + ":");
    }

    @Override
    public void visit(NotEqual n) {
        // (a != b)
        String labelTrue = "LabelNeqTrue" + n.hashCode();
        String labelEnd = "LabelNeqEnd" + n.hashCode();
        visit(n.e1);
        visit(n.e2);
        writerActual.println("   if_icmpne " + labelTrue);
        writerActual.println("   ldc 0");
        writerActual.println("   goto " + labelEnd);
        writerActual.println(labelTrue + ":");
        writerActual.println("   ldc 1");
        writerActual.println(labelEnd + ":");
    }

    @Override
    public void visit(LessThan n) {
        // (a < b)
        String labelTrue = "LabelLtTrue" + n.hashCode();
        String labelEnd = "LabelLtEnd" + n.hashCode();
        visit(n.e1);
        visit(n.e2);
        writerActual.println("   if_icmplt " + labelTrue);
        writerActual.println("   ldc 0");
        writerActual.println("   goto " + labelEnd);
        writerActual.println(labelTrue + ":");
        writerActual.println("   ldc 1");
        writerActual.println(labelEnd + ":");
    }

    @Override
    public void visit(MoreThan n) {
        // (a > b)
        String labelTrue = "LabelGtTrue" + n.hashCode();
        String labelEnd = "LabelGtEnd" + n.hashCode();
        visit(n.e1);
        visit(n.e2);
        writerActual.println("   if_icmpgt " + labelTrue);
        writerActual.println("   ldc 0");
        writerActual.println("   goto " + labelEnd);
        writerActual.println(labelTrue + ":");
        writerActual.println("   ldc 1");
        writerActual.println(labelEnd + ":");
    }

    @Override
    public void visit(ArrayLookup n) {
        // a[e2]
        visit(n.e1); // referencia al array
        visit(n.e2); // índice
        writerActual.println("   iaload");
    }

    @Override
    public void visit(ArrayLength n) {
        // a.length
        visit(n.e); // referencia al array
        writerActual.println("   arraylength");
    }

    @Override
    public void visit(Call n) {
        // e.i(el)
        // Suponemos que el objeto está en el stack
        visit(n.e); // objeto
        for (int i = 0; i < n.el.size(); i++) {
            visit(n.el.get(i)); // argumentos
        }
        // Construir descriptor de método
        StringBuilder desc = new StringBuilder();
        for (int i = 0; i < n.el.size(); i++) {
            // Si tienes tipos, usa jasminType; si no, asume int
            desc.append("I");
        }
        String methodName = n.i.s;
        String className = "";
        // Intentar deducir el tipo del objeto
        if (n.e instanceof IdentifierExpr && tipoVarLocal != null) {
            String var = ((IdentifierExpr)n.e).s;
            Type t = tipoVarLocal.get(var);
            if (t instanceof ClassType) {
                className = ((ClassType)t).className;
            } else {
                className = "TODO_Clase";
            }
        } else if (n.e instanceof This) {
            className = claseMetodoActual;
        } else if (n.e instanceof NewObject) {
            className = ((NewObject)n.e).i.s;
        } else {
            className = "TODO_Clase";
        }
        // Por defecto, asumimos retorno int
        writerActual.println("   invokevirtual " + className + "/" + methodName + "(" + desc + ")I");
    }
    @Override public void visit(Param n) {}
    @Override public void visit(IntArrayType n) {}
    @Override public void visit(IntType n) {}
    @Override public void visit(ClassType n) {}

    /**
     * Genera el código para un bloque de sentencias.
     */
    @Override
    public void visit(Block n) {
        for (int i = 0; i < n.sl.size(); i++) {
            visit(n.sl.get(i));
        }
    }

    /**
     * Genera el código para una sentencia if-else.
     */
    @Override
    public void visit(If n) {
        String labelElse = "LabelElse" + n.hashCode();
        String labelEnd = "LabelEnd" + n.hashCode();
        visit(n.e); // condición
        writerActual.println("   ifeq " + labelElse);
        visit(n.s1); // then
        writerActual.println("   goto " + labelEnd);
        writerActual.println(labelElse + ":");
        visit(n.s2); // else
        writerActual.println(labelEnd + ":");
    }

    /**
     * Genera el código para un bucle while.
     */
    @Override
    public void visit(While n) {
        String labelStart = "LabelWhileStart" + n.hashCode();
        String labelEnd = "LabelWhileEnd" + n.hashCode();
        writerActual.println(labelStart + ":");
        visit(n.e); // condición
        writerActual.println("   ifeq " + labelEnd);
        visit(n.s); // cuerpo
        writerActual.println("   goto " + labelStart);
        writerActual.println(labelEnd + ":");
    }

    /**
     * Genera el código para una asignación a un array.
     */
    @Override
    public void visit(ArrayAssign n) {
        visit(n.i);   // referencia al array
        visit(n.e1);  // índice
        visit(n.e2);  // valor
        writerActual.println("   iastore");
    }

    /**
     * Genera el código para la suma de dos enteros.
     */
    @Override
    public void visit(Plus n) {
        visit(n.e1);
        visit(n.e2);
        writerActual.println("   iadd");
    }

    /**
     * Genera el código para la resta de dos enteros.
     */
    @Override
    public void visit(Minus n) {
        visit(n.e1);
        visit(n.e2);
        writerActual.println("   isub");
    }

    /**
     * Genera el código para la multiplicación de dos enteros.
     */
    @Override
    public void visit(Mult n) {
        visit(n.e1);
        visit(n.e2);
        writerActual.println("   imul");
    }

    /**
     * Genera el código para la división de dos enteros.
     */
    @Override
    public void visit(Div n) {
        visit(n.e1);
        visit(n.e2);
        writerActual.println("   idiv");
    }

    /**
     * Genera el código para un literal entero.
     */
    @Override
    public void visit(IntegerLiteral n) {
        writerActual.println("   ldc " + n.i);
    }

    /**
     * Genera el código para la referencia a this.
     */
    @Override
    public void visit(This n) {
        writerActual.println("   aload_0");
    }

    /**
     * Genera el código para la creación de un nuevo array de enteros.
     */
    @Override
    public void visit(NewArray n) {
        visit(n.e); // tamaño
        writerActual.println("   newarray int");
    }

    /**
     * Genera el código para la creación de un nuevo objeto.
     */
    @Override
    public void visit(NewObject n) {
        writerActual.println("   new " + n.i.s);
        writerActual.println("   dup");
        writerActual.println("   invokespecial " + n.i.s + "/<init>()V");
    }

    /**
     * No genera código para un identificador aislado (solo se usa para nombres).
     */
    @Override
    public void visit(Identifier n) {
        // No genera código
    }
}

	



