package cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;

/**
 * JPA entity for the {@code watchlist} aggregate.
 *
 * <p><b>Schema migration note:</b> the previous version of this entity carried a
 * {@code user_notification_id} column (a Telegram chat id). After the introduction of
 * the Spring Modulith Notifications module, that column is no longer mapped here.
 * For an existing schema the column can be safely dropped (or left orphan); see
 * {@code doc/architecture/SPRING-MODULITH-NOTIFICATIONS-POC.md}.</p>
 */
@Entity
@Table(name = "watchlist")
public class WatchlistJpaEntity {

    @Id
    private String id;

    private String ownerName;

    private String listName;

    private boolean active;

    @OneToMany(cascade = ALL, orphanRemoval = true)
    @JoinColumn(name = "watchlist_id")
    @BatchSize(size = 50)
    private List<AlertEntryJpaEntity> alerts = new ArrayList<>();

    protected WatchlistJpaEntity() {}

    public WatchlistJpaEntity(String id, String ownerName, String listName, boolean active) {
        this.id = id;
        this.ownerName = ownerName;
        this.listName = listName;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getListName() {
        return listName;
    }

    public boolean isActive() {
        return active;
    }

    public List<AlertEntryJpaEntity> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertEntryJpaEntity> alerts) {
        this.alerts = alerts;
    }
}
