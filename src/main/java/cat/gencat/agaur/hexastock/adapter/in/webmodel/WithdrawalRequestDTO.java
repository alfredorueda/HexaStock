package cat.gencat.agaur.hexastock.adapter.in.webmodel;

import cat.gencat.agaur.hexastock.model.Money;

import java.math.BigDecimal;

public record WithdrawalRequestDTO(BigDecimal amount) {
}