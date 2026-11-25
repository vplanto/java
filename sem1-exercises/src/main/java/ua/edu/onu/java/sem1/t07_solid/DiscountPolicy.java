package ua.edu.onu.java.sem1.t07_solid;

/**
 * Стратегія розрахунку знижки (OCP / DIP).
 */
public interface DiscountPolicy {
    /**
     * Повертає фінальну суму в копійках після застосування знижки.
     */
    long apply(long subtotalCents);
}
