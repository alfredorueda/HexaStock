package cat.gencat.agaur.hexastock.notifications;

/**
 * Supported notification channels.
 *
 * <p>Add a new value here when introducing a new {@link NotificationSender} adapter
 * (for example {@code EMAIL}, {@code SMS}, {@code PUSH}).</p>
 */
public enum NotificationChannel {
    LOG,
    TELEGRAM
}
