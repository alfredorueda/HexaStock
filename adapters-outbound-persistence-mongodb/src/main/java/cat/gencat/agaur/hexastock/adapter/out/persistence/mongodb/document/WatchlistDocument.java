package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "watchlists")
public class WatchlistDocument {

    @Id
    private String id;

    private String ownerName;

    private String listName;

    private boolean active;

    private String userNotificationId;

    private List<AlertEntryDocument> alerts = new ArrayList<>();

    @Version
    private Long version;

    protected WatchlistDocument() {}

    public WatchlistDocument(String id,
                             String ownerName,
                             String listName,
                             boolean active,
                             String userNotificationId,
                             List<AlertEntryDocument> alerts) {
        this.id = id;
        this.ownerName = ownerName;
        this.listName = listName;
        this.active = active;
        this.userNotificationId = userNotificationId;
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

    public String getUserNotificationId() {
        return userNotificationId;
    }

    public List<AlertEntryDocument> getAlerts() {
        return alerts;
    }

    public Long getVersion() {
        return version;
    }
}

