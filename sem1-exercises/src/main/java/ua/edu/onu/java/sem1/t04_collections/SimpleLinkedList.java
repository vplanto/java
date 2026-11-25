package ua.edu.onu.java.sem1.t04_collections;

import java.util.NoSuchElementException;

/**
 * T04 (частина 1): Мінімальна власна реалізація однозв'язного списку.
 */
public class SimpleLinkedList<T> {

    private static final class Node<T> {
        final T value;
        Node<T> next;

        Node(T value) {
            this.value = value;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    /**
     * Додає елемент у кінець списку за O(1).
     */
    public void addLast(T element) {
        throw new UnsupportedOperationException("TODO: реалізувати addLast");
    }

    /**
     * Повертає елемент за індексом або кидає IndexOutOfBoundsException.
     */
    public T get(int index) {
        throw new UnsupportedOperationException("TODO: реалізувати get");
    }

    /**
     * Видаляє та повертає перший елемент списку або кидає NoSuchElementException, якщо список порожній.
     */
    public T removeFirst() {
        throw new UnsupportedOperationException("TODO: реалізувати removeFirst");
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
