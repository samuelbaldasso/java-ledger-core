package com.sbaldasso.ledgercore.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbaldasso.ledgercore.application.usecase.GetAccountUseCase;
import com.sbaldasso.ledgercore.application.usecase.GetStatementUseCase;
import com.sbaldasso.ledgercore.application.usecase.OpenAccountUseCase;
import com.sbaldasso.ledgercore.domain.model.Account;
import com.sbaldasso.ledgercore.domain.model.AccountNotFoundException;
import com.sbaldasso.ledgercore.domain.model.AccountNumberAlreadyExistsException;
import com.sbaldasso.ledgercore.domain.model.Money;
import com.sbaldasso.ledgercore.domain.model.Transaction;
import com.sbaldasso.ledgercore.infrastructure.web.dto.OpenAccountRequest;
import com.sbaldasso.ledgercore.infrastructure.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OpenAccountUseCase openAccountUseCase;

    @MockBean
    private GetAccountUseCase getAccountUseCase;

    @MockBean
    private GetStatementUseCase getStatementUseCase;

    @Test
    @DisplayName("Should return 201 Created with Location header when opening account succeeds")
    void shouldReturn201WhenOpeningAccount() throws Exception {
        Account account = Account.openAccount("123-45", "Samuel", BRL);
        when(openAccountUseCase.execute(any())).thenReturn(account);

        OpenAccountRequest request = new OpenAccountRequest("123-45", "Samuel", "BRL");

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/accounts/" + account.getId())))
                .andExpect(jsonPath("$.id").value(account.getId().toString()))
                .andExpect(jsonPath("$.accountNumber").value("123-45"))
                .andExpect(jsonPath("$.ownerName").value("Samuel"))
                .andExpect(jsonPath("$.balance").value(0.00))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Should return 409 Conflict when account number already exists")
    void shouldReturn409WhenAccountNumberExists() throws Exception {
        when(openAccountUseCase.execute(any()))
                .thenThrow(new AccountNumberAlreadyExistsException("123-45"));

        OpenAccountRequest request = new OpenAccountRequest("123-45", "Samuel", "BRL");

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Account Number Already Exists"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when request body fails validation")
    void shouldReturn400WhenValidationFails() throws Exception {
        OpenAccountRequest request = new OpenAccountRequest("", "", "INVALID");

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("Should return 200 OK when getting account by ID")
    void shouldReturn200WhenGettingAccountById() throws Exception {
        Account account = Account.openAccount("123-45", "Samuel", BRL);
        account.credit(Money.of("150.00", BRL));

        when(getAccountUseCase.execute(account.getId())).thenReturn(account);

        mockMvc.perform(get("/api/v1/accounts/" + account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.getId().toString()))
                .andExpect(jsonPath("$.balance").value(150.00))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    @DisplayName("Should return 404 Not Found when account does not exist")
    void shouldReturn404WhenAccountNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(getAccountUseCase.execute(id)).thenThrow(new AccountNotFoundException(id));

        mockMvc.perform(get("/api/v1/accounts/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Account Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 200 OK with statement entries for account")
    void shouldReturn200WithStatementEntries() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Transaction tx = Transaction.createTransfer(accountId, otherId, Money.of("50.00", BRL), "Transfer payment", "stmt-tx-1");

        when(getStatementUseCase.execute(accountId)).thenReturn(List.of(tx));

        mockMvc.perform(get("/api/v1/accounts/" + accountId + "/statement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.entries[0].transactionId").value(tx.getId().toString()))
                .andExpect(jsonPath("$.entries[0].type").value("DEBIT"))
                .andExpect(jsonPath("$.entries[0].amount").value(50.00));
    }
}
