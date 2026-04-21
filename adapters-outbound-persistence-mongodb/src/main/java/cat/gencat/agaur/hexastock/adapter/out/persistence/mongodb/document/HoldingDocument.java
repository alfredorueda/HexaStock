package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document;

import java.util.ArrayList;
import java.util.List;

public class HoldingDocument {
    private String id;
    private String ticker;
    private List<LotDocument> lots = new ArrayList<>();

    protected HoldingDocument() {
    }

    public HoldingDocument(String id, String ticker, List<LotDocument> lots) {
        this.id = id;
        this.ticker = ticker;
        this.lots = lots;
    }

    public String getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public List<LotDocument> getLots() {
        return lots;
    }
}
