public class ValidCode {
    public static void main(String[] args) {
        Factorial fac;
        int result;
        fac = new Factorial();
        result = fac.calculate(5);
        System.out.println(result);
    }
}

public class Factorial {
    public int calculate(int n) {
        int result;
        int i;
        result = 1;
        i = 1;
        while ((i < (n + 1))) {
            result = (result * i);
            i = (i + 1);
        }
        return result;
    }
} 