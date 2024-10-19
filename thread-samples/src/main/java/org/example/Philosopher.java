package org.example;

import java.util.concurrent.Semaphore;

class Philosopher extends Thread {

    private Semaphore sem;

    // Did the philosopher eat?
    private boolean full = false;

    private String name;

    Philosopher(Semaphore sem, String name) {
        this.sem=sem;
        this.name=name;
    }

    public void run()
    {
        try
        {
            // If the philosopher has not eaten
            if (!full) {
                // Ask the semaphore for permission to run
                sem.acquire();
                System.out.println(name + " takes a seat at the table");

                // The philosopher eats
                sleep(1000);
                full = true;

                System.out.println(name + " has eaten! He leaves the table");
                sem.release();

                // The philosopher leaves, making room for others
                sleep(1000);
            }
        }
        catch(InterruptedException e) {
            System.out.println("Something went wrong!");
        }
    }
}