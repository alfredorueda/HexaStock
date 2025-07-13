package cat.gencat.agaur.hexastock.adapter.in.webmodel;

import cat.gencat.agaur.hexastock.model.Money;

import java.math.BigDecimal;

/**
 * DepositRequestDTO is a Data Transfer Object for cash deposit requests in the REST API.
 * 
 * <p>In hexagonal architecture terms, this is part of the <strong>primary adapter</strong> layer,
 * used to receive input data from the external world for the deposit funds use case.</p>
 * 
 * <p>This DTO captures the essential information needed for a cash deposit:</p>
 * <ul>
 *   <li>The amount of money to deposit into the portfolio</li>
 * </ul>
 * 
 * <p>When a deposit request is received, the application will:</p>
 * <ol>
 *   <li>Verify the amount is positive</li>
 *   <li>Add the specified amount to the portfolio's cash balance</li>
 *   <li>Record a deposit transaction in the transaction history</li>
 * </ol>
 * 
 * <p>Deposits are a fundamental operation that allows users to fund their portfolio
 * for subsequent investment activities.</p>
 * 
 * <p>As a record, this class is immutable and provides built-in value semantics,
 * making it ideal for request DTOs that should not be modified after creation.</p>
 */
public record DepositRequestDTO(BigDecimal amount) {
}