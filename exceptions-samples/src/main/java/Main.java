public class Main {
    public static void main (String[] args) {
        int a=5;
        int b=0;
        try{
            System.out.println(a/b);
        }
        catch(ArithmeticException e){
            System.out.println(e.toString());
        }
        // Taking an empty string
        String str = null;
        // Getting length of a string
        System.out.println(str.length());

        a = 1;
        b = 0;

        // Try block to check for exceptions
        try {
            int i = computeDivision(a, b);
        }

        // Catch block to handle ArithmeticException
        // exceptions
        catch (ArithmeticException ex) {

            // getMessage() will print description
            // of exception(here / by zero)
            System.out.println(ex.getMessage());
        }


        // Taking an array of size 4
        int[] arr = new int[4];

        // Now this statement will cause an exception
        int i = arr[4];

        // This statement will never execute
        // as above we caught with an exception
        System.out.println("Hi, I want to execute");
    }

    // Method 1
    // It throws the Exception(ArithmeticException).
    // Appropriate Exception handler is not found
    // within this method.
    static int divideByZero(int a, int b)
    {

        // this statement will cause ArithmeticException
        // (/by zero)
        int i = a / b;

        return i;
    }

    // The runTime System searches the appropriate
    // Exception handler in method also but couldn't have
    // found. So looking forward on the call stack
    static int computeDivision(int a, int b)
    {

        int res = 0;

        // Try block to check for exceptions
        try {

            res = divideByZero(a, b);
        }

        // Catch block to handle NumberFormatException
        // exception Doesn't matches with
        // ArithmeticException
        catch (NumberFormatException ex) {
            // Display message when exception occurs
            System.out.println(
                    "NumberFormatException is occurred");
        }
        return res;
    }
}
