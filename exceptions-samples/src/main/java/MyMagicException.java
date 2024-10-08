// Create our own exception class by sub-classing Exception. This is a checked exception
public class MyMagicException extends Exception {
    public MyMagicException(String message) {  //constructor
        super(message);
    }
}