package cat.gencat.agaur.hexastock.adapter.in.webmodel;

/**
 * CreatePortfolioDTO is a Data Transfer Object for portfolio creation requests in the REST API.
 * 
 * <p>In hexagonal architecture terms, this is part of the <strong>primary adapter</strong> layer,
 * used to receive input data from the external world for the create portfolio use case.</p>
 * 
 * <p>This DTO captures the essential information needed to create a new portfolio:</p>
 * <ul>
 *   <li>The name of the portfolio owner</li>
 * </ul>
 * 
 * <p>When a create portfolio request is received, the application will:</p>
 * <ol>
 *   <li>Generate a unique identifier for the new portfolio</li>
 *   <li>Create a new Portfolio domain object with the owner name</li>
 *   <li>Initialize it with a zero cash balance</li>
 *   <li>Persist the new portfolio</li>
 *   <li>Return the created portfolio to the client</li>
 * </ol>
 * 
 * <p>Portfolio creation is the foundational operation that enables all other
 * financial activities in the system, as all deposits, withdrawals, and stock
 * transactions are associated with a specific portfolio.</p>
 * 
 * <p>As a record, this class is immutable and provides built-in value semantics,
 * making it ideal for request DTOs that should not be modified after creation.</p>
 */
public record CreatePortfolioDTO(String ownerName) {
}