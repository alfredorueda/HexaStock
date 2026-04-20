package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.document;

import java.util.ArrayList;
import java.util.List;

/**
 * Embedded holding sub-document inside {@link PortfolioDocument}.
 *
 * <p>Adapter-local representation of a single ticker holding with its purchase lots.
 * Because the Portfolio aggregate maps to a single MongoDB document, updates to
 * holdings and lots are atomic together with the owning portfolio document — no
 * multi-document transaction is required.</p>
 */
public class HoldingDocument {

    private String id;
    private String ticker;
    private List<LotDocument> lots = new ArrayList<>();

    protected HoldingDocument() { /* for MongoDB mapper */ }

    public HoldingDocument(String id, String ticker, List<LotDocument> lots) {
        this.id = id;
        this.ticker = ticker;
        this.lots = lots != null ? lots : new ArrayList<>();
    }

    public String getId() { return id; }
    public String getTicker() { return ticker; }
    public List<LotDocument> getLots() { return lots; }
}
