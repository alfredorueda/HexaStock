package cat.gencat.agaur.hexastock.model;

/**
 * TransactionType represents the different kinds of financial activities in a portfolio.
 * 
 * In DDD terms, this is an Enumeration Value Object that categorizes transactions.
 * 
 * The system supports four types of transactions:
 * - DEPOSIT: Adding money to the portfolio's cash balance
 * - WITHDRAWAL: Taking money out of the portfolio's cash balance
 * - PURCHASE: Buying shares of a stock, which decreases cash and adds to holdings
 * - SALE: Selling shares of a stock, which increases cash and reduces holdings
 * 
 * These transaction types help organize the financial history and enable
 * filtering and analysis of investment activities.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    PURCHASE,
    SALE
}
