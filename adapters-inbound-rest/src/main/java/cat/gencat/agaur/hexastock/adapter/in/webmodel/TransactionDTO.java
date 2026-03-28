package cat.gencat.agaur.hexastock.adapter.in.webmodel;

import cat.gencat.agaur.hexastock.model.Transaction;

/**
 * TransactionDTO is a Data Transfer Object for transaction information in the REST API.
 * 
 * <p>In hexagonal architecture terms, this is part of the <strong>primary adapter</strong> layer,
 * used to transfer data between the domain model and the external world. It wraps the domain
 * Transaction object to expose it to API consumers.</p>
 * 
 * <p>This DTO encapsulates information about a financial transaction, including:</p>
 * <ul>
 *   <li>Transaction type (deposit, withdrawal, purchase, or sale)</li>
 *   <li>Date and time of the transaction</li>
 *   <li>Financial details (amounts, prices, quantities)</li>
 *   <li>Stock information for purchase and sale transactions</li>
 *   <li>Profit/loss information for sale transactions</li>
 * </ul>
 * 
 * <p>This information enables users to review their financial history and track their
 * investment activities and performance over time.</p>
 * 
 * <p>As a record, this class is immutable and provides built-in value semantics,
 * which is ideal for DTOs that should not be modified after creation.</p>
 */
public record TransactionDTO(Transaction transaction) {
}