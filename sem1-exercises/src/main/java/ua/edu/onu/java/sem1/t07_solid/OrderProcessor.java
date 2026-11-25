package ua.edu.onu.java.sem1.t07_solid;

/**
 * T07: Високорівневий модуль залежить лише від абстракцій (DIP + Constructor Injection).
 */
public class OrderProcessor {

    private final DiscountPolicy discountPolicy;
    private final Notifier notifier;

    public OrderProcessor(DiscountPolicy discountPolicy, Notifier notifier) {
        if (discountPolicy == null || notifier == null) {
            throw new IllegalArgumentException("Залежності не можуть бути null");
        }
        this.discountPolicy = discountPolicy;
        this.notifier = notifier;
    }

    /**
     * Обчислює фінальну суму через discountPolicy, надсилає сповіщення клієнту
     * формату "Order <orderId> total: <finalAmountCents>" і повертає finalAmountCents.
     * Якщо customer або orderId порожні, або subtotalCents < 0 - кидає IllegalArgumentException.
     */
    public long checkout(String orderId, String customer, long subtotalCents) {
        throw new UnsupportedOperationException("TODO: реалізувати checkout через DI-залежності");
    }
}
