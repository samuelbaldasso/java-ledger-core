package com.sbaldasso.ledgercore.infrastructure.web.controller;

import com.sbaldasso.ledgercore.application.usecase.GetAccountUseCase;
import com.sbaldasso.ledgercore.application.usecase.GetStatementUseCase;
import com.sbaldasso.ledgercore.application.usecase.OpenAccountCommand;
import com.sbaldasso.ledgercore.application.usecase.OpenAccountUseCase;
import com.sbaldasso.ledgercore.domain.model.Account;
import com.sbaldasso.ledgercore.domain.model.Transaction;
import com.sbaldasso.ledgercore.infrastructure.web.dto.AccountResponse;
import com.sbaldasso.ledgercore.infrastructure.web.dto.AccountStatementResponse;
import com.sbaldasso.ledgercore.infrastructure.web.dto.OpenAccountRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Operações de abertura, consulta de saldo e emissão de extrato contábil")
public class AccountController {

    private final OpenAccountUseCase openAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final GetStatementUseCase getStatementUseCase;

    public AccountController(
            OpenAccountUseCase openAccountUseCase,
            GetAccountUseCase getAccountUseCase,
            GetStatementUseCase getStatementUseCase
    ) {
        this.openAccountUseCase = openAccountUseCase;
        this.getAccountUseCase = getAccountUseCase;
        this.getStatementUseCase = getStatementUseCase;
    }

    @PostMapping
    @Operation(summary = "Abre uma nova conta bancária", description = "Cria uma conta com saldo zerado e status ACTIVE na moeda informada.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada com sucesso",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Número de conta já cadastrado no sistema",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest request) {
        Currency currency = Currency.getInstance(request.currency().toUpperCase());
        OpenAccountCommand command = new OpenAccountCommand(
                request.accountNumber(),
                request.ownerName(),
                currency
        );

        Account account = openAccountUseCase.execute(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(account.getId())
                .toUri();

        return ResponseEntity.created(location).body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta os dados e saldo consolidado de uma conta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta encontrada",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        Account account = getAccountUseCase.execute(id);
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @GetMapping("/{id}/statement")
    @Operation(summary = "Emite o extrato contábil dos lançamentos no Livro-Razão para a conta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Extrato emitido com sucesso",
                    content = @Content(schema = @Schema(implementation = AccountStatementResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AccountStatementResponse> getStatement(@PathVariable UUID id) {
        List<Transaction> transactions = getStatementUseCase.execute(id);
        return ResponseEntity.ok(AccountStatementResponse.from(id, transactions));
    }
}
