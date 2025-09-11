package cat.gencat.agaur.hexastock.adapter.in.webmodel;

import java.math.BigDecimal;

/**
 * WithdrawalRequestDTO is a Data Transfer Object for cash withdrawal requests in the REST API.
 * 
 * <p>In hexagonal architecture terms, this is part of the <strong>primary adapter</strong> layer,
 * used to receive input data from the external world for the withdraw funds use case.</p>
 * 
 * <p>This DTO captures the essential information needed for a cash withdrawal:</p>
 * <ul>
 *   <li>The amount of money to withdraw from the portfolio</li>
 * </ul>
 * 
 * <p>When a withdrawal request is received, the application will:</p>
 * <ol>
 *   <li>Verify the amount is positive</li>
 *   <li>Check that the portfolio has sufficient funds</li>
 *   <li>Deduct the specified amount from the portfolio's cash balance</li>
 *   <li>Record a withdrawal transaction in the transaction history</li>
 * </ol>
 * 
 * <p>Withdrawals allow users to extract funds from their portfolio, either to realize
 * investment gains or to repurpose funds for other financial needs.</p>
 * 
 * <p>As a record, this class is immutable and provides built-in value semantics,
 * making it ideal for request DTOs that should not be modified after creation.</p>
 */
public record WithdrawalRequestDTO(BigDecimal amount) {
}