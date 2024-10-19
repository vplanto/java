package org.example;

import java.util.concurrent.Semaphore;

public class PhilosopherDining {
    public static void main(String[] args) {

        Semaphore sem = new Semaphore(2,true);
        new Philosopher(sem, "Socrates").start();
        new Philosopher(sem,"Plato").start();
        new Philosopher(sem,"Aristotle").start();
        new Philosopher(sem, "Thales").start();
        new Philosopher(sem, "Pythagoras").start();
    }
}
