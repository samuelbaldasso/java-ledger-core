package com.sbaldasso.ledgercore.application.usecase;

import com.sbaldasso.ledgercore.domain.model.Account;

import java.util.UUID;

public interface GetAccountUseCase {

    Account execute(UUID accountId);
}
