package ua.edu.onu.java.sem1.t06_exceptions;

/**
 * T06: Обробка винятків та захист інваріантів (баланс зберігається у копійках).
 */
public class WalletAccount {

    private long balanceCents;

    public WalletAccount(long initialBalanceCents) {
        if (initialBalanceCents < 0) {
            throw new IllegalArgumentException("Початковий баланс не може бути від'ємним");
        }
        this.balanceCents = initialBalanceCents;
    }

    public long getBalanceCents() {
        return balanceCents;
    }

    /**
     * Поповнення на суму > 0. Інакше кидає IllegalArgumentException.
     */
    public void deposit(long amountCents) {
        throw new UnsupportedOperationException("TODO: реалізувати deposit");
    }

    /**
     * Зняття суми > 0. Якщо amountCents <= 0 - IllegalArgumentException.
     * Якщо коштів недостатньо - InsufficientFundsException (баланс не змінюється).
     */
    public void withdraw(long amountCents) throws InsufficientFundsException {
        throw new UnsupportedOperationException("TODO: реалізувати withdraw");
    }

    /**
     * Атомарний переказ на інший рахунок. Якщо target == null або target == this - IllegalArgumentException.
     */
    public void transferTo(WalletAccount target, long amountCents) throws InsufficientFundsException {
        throw new UnsupportedOperationException("TODO: реалізувати transferTo");
    }
}
