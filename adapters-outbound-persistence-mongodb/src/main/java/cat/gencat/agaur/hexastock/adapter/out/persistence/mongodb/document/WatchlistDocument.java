package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document for the {@code watchlists} collection.
 *
 * <p><b>Schema migration note:</b> the previous version carried a
 * {@code userNotificationId} field (Telegram chat id). After the introduction of the
 * Spring Modulith Notifications module, the field is no longer mapped here.
 * Existing documents may still contain it; MongoDB happily ignores unmapped fields,
 * so no destructive migration is required for the POC.</p>
 */
@Document(collection = "watchlists")
public class WatchlistDocument {

    @Id
    private String id;

    private String ownerName;

    private String listName;

    private boolean active;

    private List<AlertEntryDocument> alerts = new ArrayList<>();

    @Version
    private Long version;

    protected WatchlistDocument() {}

    public WatchlistDocument(String id,
                             String ownerName,
                             String listName,
                             boolean active,
                             List<AlertEntryDocument> alerts) {
        this.id = id;
        this.ownerName = ownerName;
        this.listName = listName;
        this.active = active;
        this.alerts = alerts;
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

    public List<AlertEntryDocument> getAlerts() {
        return alerts;
    }

    public Long getVersion() {
        return version;
    }
}
