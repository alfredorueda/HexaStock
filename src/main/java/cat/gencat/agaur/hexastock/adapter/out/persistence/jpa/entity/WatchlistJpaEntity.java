package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "watchlist")
public class WatchlistJpaEntity {

    @Id
    private String id;

    private String ownerName;

    private String listName;

    private boolean active;

    @OneToMany(mappedBy = "watchlist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AlertEntryJpaEntity> alerts = new ArrayList<>();

    protected WatchlistJpaEntity() {
    }

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
