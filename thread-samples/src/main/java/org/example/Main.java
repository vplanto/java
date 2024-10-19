package org.example;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        int a = 1;
        int b = 1;
        System.out.println(a==b);
        a=128;
        b=128;
        System.out.println(a==b);
        Integer c = 1;
        Integer d = 1;
        System.out.println(c==d);
        c = 128;
        d = 128;
        System.out.println(c==d);

        new Thread(new HelloRunnable()).start();
        new Thread(new HelloThread()).start();
    }
}