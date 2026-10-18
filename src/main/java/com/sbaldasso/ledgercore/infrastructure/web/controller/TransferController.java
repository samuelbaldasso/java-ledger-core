package com.sbaldasso.ledgercore.infrastructure.web.controller;

import com.sbaldasso.ledgercore.application.usecase.TransferMoneyCommand;
import com.sbaldasso.ledgercore.application.usecase.TransferMoneyUseCase;
import com.sbaldasso.ledgercore.application.usecase.TransferResult;
import com.sbaldasso.ledgercore.domain.model.Money;
import com.sbaldasso.ledgercore.infrastructure.web.dto.TransferRequest;
import com.sbaldasso.ledgercore.infrastructure.web.dto.TransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Currency;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers", description = "Operações financeiras de transferência contábil no Livro-Razão (Double-Entry)")
public class TransferController {

    private final TransferMoneyUseCase transferMoneyUseCase;

    public TransferController(TransferMoneyUseCase transferMoneyUseCase) {
        this.transferMoneyUseCase = transferMoneyUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Executa uma transferência bancária com idempotência e partidas dobradas",
            description = "Debita a conta de origem, credita a conta de destino e registra as entradas contábeis no Ledger de forma atômica. " +
                    "Requer uma chave de idempotência no header HTTP."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transferência executada ou retornada do cache de idempotência com sucesso",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requisição malformada, falta de header ou divergência de moeda",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Conta de origem ou destino não encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Chave reutilizada com dados diferentes ou resposta histórica indisponível",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente ou conta inativa",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TransferResponse> transfer(
            @Parameter(description = "Identificador único da intenção de pagamento (UUID)", required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {
        Currency currency = Currency.getInstance(request.currency().toUpperCase(Locale.ROOT));
        Money amount = Money.of(request.amount(), currency);

        TransferMoneyCommand command = new TransferMoneyCommand(
                request.sourceAccountId(),
                request.targetAccountId(),
                amount,
                request.description(),
                idempotencyKey
        );

        TransferResult result = transferMoneyUseCase.execute(command);
        return ResponseEntity.ok(TransferResponse.from(result));
    }
}
