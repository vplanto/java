package org.example;

public class MyLambda {
    public static void main(String[] args) {
        Runnable runnable =
                () -> { System.out.println("Lambda Runnable running"); };
        Thread thread = new Thread(runnable);
        thread.start();
    }
}
