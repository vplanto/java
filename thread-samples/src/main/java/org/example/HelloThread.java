package org.example;

public class HelloThread extends Thread {
    @Override
    public void run() {
        System.out.println("Hello from a thread extended from Thread!");
    }
}