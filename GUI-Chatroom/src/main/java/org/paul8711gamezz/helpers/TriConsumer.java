package org.paul8711gamezz.helpers;

/**
 * Functional interface that accepts three arguments.
 */
@FunctionalInterface
public interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
}