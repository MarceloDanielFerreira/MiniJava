public class InvalidCode {
    public static void main(String[] args) {
        // Variable no utilizada
        int unusedVar;
        unusedVar = 10;
        
        // Variables duplicadas
        int x;
        String x;
        x = 5;
        x = "error";
        
        // Type checking incorrecto
        Factorial fac;
        fac = new Factorial();
        fac = 10;  // Error: asignando int a Factorial
        int y;
        y = true;  // Error: asignando boolean a int
        
        // Método no existente
        fac.nonExistentMethod();
        
        // Parámetros incorrectos
        int result;
        result = fac.calculate("error");  // Error: pasando String a método que espera int
        
        // Retorno incorrecto
        int wrong;
        wrong = fac.wrongReturn();
    }
}

public class Factorial {
    public int calculate(int n) {
        // Variable no utilizada
        int unused;
        unused = 0;
        
        // Type checking en expresiones
        int x;
        x = (10 + "error");  // Error: operando String con int
        
        // Retorno incorrecto
        return "error";  // Error: retornando String en método int
    }
    
    public int wrongReturn() {
        Factorial f;
        f = new Factorial();
        return f;  // Error: retornando Factorial en método int
    }
} 