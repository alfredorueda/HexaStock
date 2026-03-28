package cat.gencat.agaur.hexastock.model.exception;

/**
 * EntityExistsException indicates that an attempt was made to create or add an entity
 * that already exists in the system.
 * 
 * <p>In DDD terms, this domain exception enforces the uniqueness constraint for entities.
 * It ensures that duplicate entities are not created or added to aggregates.</p>
 * 
 * <p>This exception is thrown when:</p>
 * <ul>
 *   <li>A Holding with the same ticker is being added to a Portfolio that already contains it</li>
 *   <li>A Lot with the same ID is being added to a Holding that already contains it</li>
 * </ul>
 * 
 * <p>It represents a business rule that maintains data integrity by preventing duplicate
 * entities within aggregates.</p>
 */
public class HoldingNotFoundException extends DomainException {
    /**
     * Constructs a new EntityExistsException with the specified detail message.
     *
     * @param message The detail message explaining which entity already exists
     */
    public HoldingNotFoundException(String message) {
        super(message);
    }
}
