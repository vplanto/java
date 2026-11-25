package ua.edu.onu.java.sem1.t06_exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionsTest {

    @Test
    void depositAndWithdrawProtectInvariants() throws InsufficientFundsException {
        WalletAccount acc = new WalletAccount(1000);
        acc.deposit(500);
        assertEquals(1500, acc.getBalanceCents());

        acc.withdraw(400);
        assertEquals(1100, acc.getBalanceCents());

        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () -> acc.withdraw(2000));
        assertEquals(2000, ex.getRequested());
        assertEquals(1100, ex.getAvailable());
        assertEquals(1100, acc.getBalanceCents());

        assertThrows(IllegalArgumentException.class, () -> acc.deposit(0));
        assertThrows(IllegalArgumentException.class, () -> acc.withdraw(-10));
    }

    @Test
    void transferMovesFundsOrLeavesBothUnchanged() throws InsufficientFundsException {
        WalletAccount a = new WalletAccount(1000);
        WalletAccount b = new WalletAccount(200);

        a.transferTo(b, 300);
        assertEquals(700, a.getBalanceCents());
        assertEquals(500, b.getBalanceCents());

        assertThrows(InsufficientFundsException.class, () -> a.transferTo(b, 900));
        assertEquals(700, a.getBalanceCents());
        assertEquals(500, b.getBalanceCents());

        assertThrows(IllegalArgumentException.class, () -> a.transferTo(null, 100));
        assertThrows(IllegalArgumentException.class, () -> a.transferTo(a, 100));
    }
}
