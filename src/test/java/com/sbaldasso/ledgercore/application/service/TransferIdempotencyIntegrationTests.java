package com.sbaldasso.ledgercore.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbaldasso.ledgercore.TestcontainersConfiguration;
import com.sbaldasso.ledgercore.application.idempotency.IdempotencyConflictException;
import com.sbaldasso.ledgercore.application.idempotency.IdempotencyResultUnavailableException;
import com.sbaldasso.ledgercore.application.idempotency.TransferReceipt;
import com.sbaldasso.ledgercore.application.usecase.TransferMoneyCommand;
import com.sbaldasso.ledgercore.application.usecase.TransferMoneyUseCase;
import com.sbaldasso.ledgercore.application.usecase.TransferResult;
import com.sbaldasso.ledgercore.domain.model.AccountNotActiveException;
import com.sbaldasso.ledgercore.domain.model.Money;
import com.sbaldasso.ledgercore.infrastructure.persistence.adapter.TransactionRepositoryAdapter;
import com.sbaldasso.ledgercore.infrastructure.persistence.adapter.TransferReceiptRepositoryAdapter;
import com.sbaldasso.ledgercore.infrastructure.web.dto.TransferRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Timeout(45)
class TransferIdempotencyIntegrationTests {
    @Autowired
    private TransferMoneyUseCase transfers;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @SpyBean
    private TransactionRepositoryAdapter transactions;
    @SpyBean
    private TransferReceiptRepositoryAdapter receipts;

    @Test
    void shouldReplayExactHttpResponseAfterAnotherTransferAndAccountClosure() throws Exception {
        UUID source = account("100", "ACTIVE");
        UUID target = account("0", "ACTIVE");
        String key = UUID.randomUUID().toString();
        TransferRequest request = new TransferRequest(source, target, new BigDecimal("100.0"), "BRL", "Payment");
        String original = postTransfer(key, request);
        transfers.execute(command(target, source, "25", UUID.randomUUID().toString()));
        jdbc.update("UPDATE accounts SET status = 'CLOSED' WHERE id = ?", target);

        TransferRequest retry = new TransferRequest(source, target, new BigDecimal("100.00"), "brl", "Payment");
        assertThat(postTransfer(key, retry)).isEqualTo(original);
        assertBalance(source, "25");
        assertBalance(target, "75");
        assertRecords(key, 1, 2, 1);
    }

    @Test
    void shouldReturn409ForChangedAmountWithoutChangingBalances() throws Exception {
        UUID source = account("1000", "ACTIVE");
        UUID target = account("0", "ACTIVE");
        String key = UUID.randomUUID().toString();
        transfers.execute(command(source, target, "100", key));

        var retry = new TransferRequest(source, target, new BigDecimal("900"), "BRL", "Payment");
        mvc.perform(post("/api/v1/transfers").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(retry)))
                .andExpect(status().isConflict());
        assertBalance(source, "900");
        assertBalance(target, "100");
        assertRecords(key, 1, 2, 1);
    }

    @Test
    void shouldReplayConcurrentRetryEvenWhenWinnerExhaustsBalance() throws Exception {
        UUID source = account("100", "ACTIVE");
        UUID target = account("0", "ACTIVE");
        String key = UUID.randomUUID().toString();
        synchronizeInitialReads(key);
        TransferMoneyCommand command = command(source, target, "100", key);

        List<Object> outcomes = race(command, command);
        assertThat(outcomes.get(0)).isInstanceOf(TransferResult.class);
        assertThat(outcomes.get(1)).isEqualTo(outcomes.get(0));
        assertBalance(source, "0");
        assertBalance(target, "100");
        assertRecords(key, 1, 2, 1);
    }

    @Test
    void shouldRollbackLosingTransferOnConcurrentKeyCollisionWithDifferentAccounts() throws Exception {
        UUID sourceA = account("1000", "ACTIVE");
        UUID targetA = account("0", "ACTIVE");
        UUID sourceB = account("1000", "ACTIVE");
        UUID targetB = account("0", "ACTIVE");
        String key = UUID.randomUUID().toString();
        // Both executions reach persistence after mutating distinct account pairs.
        CyclicBarrier writes = new CyclicBarrier(2);
        doAnswer(invocation -> {
            TransferReceipt receipt = invocation.getArgument(0);
            if (receipt.request().idempotencyKey().equals(key)) {
                writes.await(10, TimeUnit.SECONDS);
            }
            return invocation.callRealMethod();
        }).when(receipts).insert(any());

        List<Object> outcomes = race(command(sourceA, targetA, "100", key), command(sourceB, targetB, "900", key));
        assertThat(outcomes.stream().filter(TransferResult.class::isInstance).count()).isEqualTo(1);
        assertThat(outcomes.stream().filter(IdempotencyConflictException.class::isInstance).count()).isEqualTo(1);
        TransferResult winner = (TransferResult) outcomes.stream().filter(TransferResult.class::isInstance).findFirst().orElseThrow();
        if (winner.sourceAccountId().equals(sourceA)) {
            assertBalance(sourceA, "900");
            assertBalance(targetA, "100");
            assertBalance(sourceB, "1000");
            assertBalance(targetB, "0");
        } else {
            assertBalance(sourceA, "1000");
            assertBalance(targetA, "0");
            assertBalance(sourceB, "100");
            assertBalance(targetB, "900");
        }
        assertRecords(key, 1, 2, 1);
    }

    @Test
    void shouldRollbackBalancesLedgerAndReceiptWhenFailureOccursAfterAllWrites() {
        UUID source = account("100", "ACTIVE");
        UUID target = account("0", "ACTIVE");
        String key = UUID.randomUUID().toString();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("Injected failure after flush");
        }).when(receipts).insert(any());

        assertThatThrownBy(() -> transfers.execute(command(source, target, "50", key)))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasMessage("Injected failure after flush");
        assertBalance(source, "100");
        assertBalance(target, "0");
        assertRecords(key, 0, 0, 0);
    }

    @Test
    void shouldRollbackDebitWhenTargetIsInactive() {
        UUID source = account("100", "ACTIVE");
        UUID target = account("0", "CLOSED");
        String key = UUID.randomUUID().toString();

        assertThatThrownBy(() -> transfers.execute(command(source, target, "50", key)))
                .isInstanceOf(AccountNotActiveException.class);
        assertBalance(source, "100");
        assertBalance(target, "0");
        assertRecords(key, 0, 0, 0);
    }

    @Test
    void shouldPreserveHistoricalKeysWithoutInventingAnOriginalResponse() {
        UUID source = account("100", "ACTIVE");
        UUID target = account("0", "ACTIVE");
        String key = UUID.randomUUID().toString();
        TransferMoneyCommand command = command(source, target, "50", key);
        transfers.execute(command);
        // Simulate a transaction created before V3, when no receipt existed.
        jdbc.update("DELETE FROM transfer_receipts WHERE idempotency_key = ?", key);

        assertThatThrownBy(() -> transfers.execute(command)).isInstanceOf(IdempotencyResultUnavailableException.class);
        assertBalance(source, "50");
        assertBalance(target, "50");
        assertRecords(key, 1, 2, 0);
    }

    @Test
    void shouldNotSilentlyRoundDifferentRequestAmountsIntoSameIntent() throws Exception {
        var request = new TransferRequest(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.001"), "BRL", "Payment");
        mvc.perform(post("/api/v1/transfers").header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private void synchronizeInitialReads(String key) {
        CyclicBarrier reads = new CyclicBarrier(2);
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            reads.await(10, TimeUnit.SECONDS);
            return result;
        }).when(transactions).findByIdempotencyKey(key);
    }

    private List<Object> race(TransferMoneyCommand first, TransferMoneyCommand second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> outcome(first));
            var b = executor.submit(() -> outcome(second));
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Object outcome(TransferMoneyCommand command) {
        try {
            return transfers.execute(command);
        } catch (RuntimeException ex) {
            return ex;
        }
    }

    private UUID account(String balance, String status) {
        UUID id = UUID.randomUUID();
        // Funded fixture; this test does not implement the future ledger funding use case.
        jdbc.update("""
                INSERT INTO accounts (id, account_number, owner_name, balance, currency, status, created_at)
                VALUES (?, ?, 'Fixture', ?, 'BRL', ?, CURRENT_TIMESTAMP)
                """, id, id.toString().replace("-", ""), new BigDecimal(balance), status);
        return id;
    }

    private TransferMoneyCommand command(UUID source, UUID target, String amount, String key) {
        return new TransferMoneyCommand(source, target, Money.of(amount, "BRL"), "Payment", key);
    }

    private String postTransfer(String key, TransferRequest request) throws Exception {
        return mvc.perform(post("/api/v1/transfers").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private void assertBalance(UUID account, String expected) {
        assertThat(jdbc.queryForObject("SELECT balance FROM accounts WHERE id = ?", BigDecimal.class, account))
                .isEqualByComparingTo(expected);
    }

    private void assertRecords(String key, int transactionCount, int entryCount, int receiptCount) {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transactions WHERE idempotency_key = ?", Integer.class, key))
                .isEqualTo(transactionCount);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM entries e JOIN transactions t ON e.transaction_id = t.id
                WHERE t.idempotency_key = ?
                """, Integer.class, key)).isEqualTo(entryCount);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transfer_receipts WHERE idempotency_key = ?", Integer.class, key))
                .isEqualTo(receiptCount);
        assertThat(jdbc.queryForObject("""
                SELECT coalesce(sum(CASE WHEN e.entry_type = 'CREDIT' THEN e.amount ELSE -e.amount END), 0)
                FROM entries e JOIN transactions t ON e.transaction_id = t.id WHERE t.idempotency_key = ?
                """, BigDecimal.class, key)).isEqualByComparingTo("0");
    }
}
