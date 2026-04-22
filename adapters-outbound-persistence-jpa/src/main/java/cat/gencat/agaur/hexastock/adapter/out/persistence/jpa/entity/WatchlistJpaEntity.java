package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "watchlist")
public class WatchlistJpaEntity {

    @Id
    private String id;

    private String ownerName;

    private String listName;

    private boolean active;

    private String telegramChatId;

    @OneToMany(cascade = ALL, orphanRemoval = true)
    @JoinColumn(name = "watchlist_id")
    @BatchSize(size = 50)
    private List<AlertEntryJpaEntity> alerts = new ArrayList<>();

    protected WatchlistJpaEntity() {}

    public WatchlistJpaEntity(String id, String ownerName, String listName, boolean active, String telegramChatId) {
        this.id = id;
        this.ownerName = ownerName;
        this.listName = listName;
        this.active = active;
        this.telegramChatId = telegramChatId;
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

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public List<AlertEntryJpaEntity> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertEntryJpaEntity> alerts) {
        this.alerts = alerts;
    }
}

