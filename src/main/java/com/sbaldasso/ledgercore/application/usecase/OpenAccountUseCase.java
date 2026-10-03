package com.sbaldasso.ledgercore.application.usecase;

import com.sbaldasso.ledgercore.domain.model.Account;

public interface OpenAccountUseCase {

    Account execute(OpenAccountCommand command);
}
