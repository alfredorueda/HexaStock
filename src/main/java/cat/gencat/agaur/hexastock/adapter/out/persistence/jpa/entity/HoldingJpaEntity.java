package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "holding")
public class HoldingJpaEntity {
    @Id 
    private String id;
    
    private String ticker;

    @OneToMany(cascade = ALL, orphanRemoval = true)
    @JoinColumn(name = "holding_id")
    @OrderBy("purchasedAt ASC")
    private List<LotJpaEntity> lots = new ArrayList<>();

    protected HoldingJpaEntity() {}
    
    public HoldingJpaEntity(String id, String ticker) {
        this.id = id;
        this.ticker = ticker;
    }

    public String getId() {
        return id;
    }
    
    public String getTicker() {
        return ticker;
    }
    
    public List<LotJpaEntity> getLots() {
        return lots;
    }

    public void setLots(List<LotJpaEntity> lots) {
        this.lots = lots;
    }
}
