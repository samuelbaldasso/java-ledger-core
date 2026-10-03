package com.sbaldasso.ledgercore.application.service;

import com.sbaldasso.ledgercore.application.usecase.OpenAccountCommand;
import com.sbaldasso.ledgercore.domain.model.Account;
import com.sbaldasso.ledgercore.domain.model.AccountNumberAlreadyExistsException;
import com.sbaldasso.ledgercore.domain.model.Money;
import com.sbaldasso.ledgercore.domain.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAccountServiceTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Mock
    private AccountRepository accountRepository;

    private OpenAccountService openAccountService;

    @BeforeEach
    void setUp() {
        openAccountService = new OpenAccountService(accountRepository);
    }

    @Test
    @DisplayName("Should open account successfully when account number is unique")
    void shouldOpenAccountSuccessfully() {
        when(accountRepository.findByAccountNumber("12345-6")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OpenAccountCommand command = new OpenAccountCommand("12345-6", "Samuel", BRL);

        Account account = openAccountService.execute(command);

        assertThat(account).isNotNull();
        assertThat(account.getAccountNumber()).isEqualTo("12345-6");
        assertThat(account.getOwnerName()).isEqualTo("Samuel");
        assertThat(account.getBalance()).isEqualTo(Money.zero(BRL));
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw AccountNumberAlreadyExistsException when account number already registered")
    void shouldThrowWhenAccountNumberAlreadyExists() {
        Account existing = Account.openAccount("12345-6", "Other", BRL);
        when(accountRepository.findByAccountNumber("12345-6")).thenReturn(Optional.of(existing));

        OpenAccountCommand command = new OpenAccountCommand("12345-6", "Samuel", BRL);

        assertThatThrownBy(() -> openAccountService.execute(command))
                .isInstanceOf(AccountNumberAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(accountRepository, never()).save(any());
    }
}
