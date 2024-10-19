package org.example;

public class WaitNotifyExample {
    public static void main(String[] args) {
        Object lock = new Object();

        Thread waiter = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("Waiting thread is waiting for the signal.");
                    lock.wait(); // Waiting thread pauses here
                    System.out.println("Waiting thread got the signal and is running again.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread notifier = new Thread(() -> {
            synchronized (lock) {
                System.out.println("Notifier thread is sending a signal.");
                lock.notify(); // Notifier thread sends the signal
                System.out.println("Notifier thread has sent the signal.");
            }
        });

        waiter.start();
        try {
            Thread.sleep(1000); // Ensures waiter starts waiting before notifier sends notification
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        notifier.start();
    }
}
