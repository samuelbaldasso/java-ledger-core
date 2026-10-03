package com.sbaldasso.ledgercore.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbaldasso.ledgercore.application.usecase.TransferMoneyUseCase;
import com.sbaldasso.ledgercore.application.usecase.TransferResult;
import com.sbaldasso.ledgercore.domain.model.AccountNotFoundException;
import com.sbaldasso.ledgercore.domain.model.CurrencyMismatchException;
import com.sbaldasso.ledgercore.domain.model.InsufficientFundsException;
import com.sbaldasso.ledgercore.domain.model.Money;
import com.sbaldasso.ledgercore.infrastructure.web.dto.TransferRequest;
import com.sbaldasso.ledgercore.infrastructure.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@Import(GlobalExceptionHandler.class)
class TransferControllerTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransferMoneyUseCase transferMoneyUseCase;

    @Test
    @DisplayName("Should return 200 OK when transfer succeeds")
    void shouldReturn200WhenTransferSucceeds() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Instant now = Instant.now();

        TransferResult result = new TransferResult(
                txId,
                sourceId,
                targetId,
                Money.of("50.00", BRL),
                Money.of("150.00", BRL),
                Money.of("100.00", BRL),
                now
        );

        when(transferMoneyUseCase.execute(any())).thenReturn(result);

        TransferRequest request = new TransferRequest(
                sourceId,
                targetId,
                new BigDecimal("50.00"),
                "BRL",
                "Dinner"
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "test-key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(txId.toString()))
                .andExpect(jsonPath("$.sourceAccountId").value(sourceId.toString()))
                .andExpect(jsonPath("$.targetAccountId").value(targetId.toString()))
                .andExpect(jsonPath("$.transferredAmount").value(50.00))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.sourceNewBalance").value(150.00))
                .andExpect(jsonPath("$.targetNewBalance").value(100.00));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when Idempotency-Key header is missing")
    void shouldReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        TransferRequest request = new TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("50.00"),
                "BRL",
                "Dinner"
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Missing Request Header"))
                .andExpect(jsonPath("$.headerName").value("Idempotency-Key"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when amount is non-positive or missing")
    void shouldReturn400WhenValidationFails() throws Exception {
        TransferRequest request = new TransferRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("-10.00"),
                "BRL",
                "Dinner"
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "test-key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    @DisplayName("Should return 422 Unprocessable Entity when InsufficientFundsException is thrown")
    void shouldReturn422WhenInsufficientFunds() throws Exception {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        when(transferMoneyUseCase.execute(any()))
                .thenThrow(new InsufficientFundsException(sourceId, Money.of("100.00", BRL), Money.of("20.00", BRL)));

        TransferRequest request = new TransferRequest(
                sourceId,
                targetId,
                new BigDecimal("100.00"),
                "BRL",
                "Rent payment"
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "test-key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Insufficient Funds"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.requested").value(100.00))
                .andExpect(jsonPath("$.currentBalance").value(20.00));
    }

    @Test
    @DisplayName("Should return 404 Not Found when AccountNotFoundException is thrown")
    void shouldReturn404WhenAccountNotFound() throws Exception {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        when(transferMoneyUseCase.execute(any()))
                .thenThrow(new AccountNotFoundException(sourceId));

        TransferRequest request = new TransferRequest(
                sourceId,
                targetId,
                new BigDecimal("50.00"),
                "BRL",
                "Transfer"
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "test-key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Account Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when CurrencyMismatchException is thrown")
    void shouldReturn400WhenCurrencyMismatch() throws Exception {
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        when(transferMoneyUseCase.execute(any()))
                .thenThrow(new CurrencyMismatchException(BRL, Currency.getInstance("USD")));

        TransferRequest request = new TransferRequest(
                sourceId,
                targetId,
                new BigDecimal("50.00"),
                "BRL",
                "Transfer"
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "test-key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Currency Mismatch"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
