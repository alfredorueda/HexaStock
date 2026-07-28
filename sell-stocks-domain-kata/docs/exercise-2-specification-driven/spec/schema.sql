-- =====================================================================
-- Portfolio domain — the same model as tables
--
-- The entity-relationship view of domain-model.md, written as executable
-- DDL rather than as a picture: every constraint here is real and can be
-- run. A drawn version of the same three tables is in domain-diagrams.md.
--
-- Nothing in the Java project reads this file. It exists so the model can
-- be read by anyone who thinks in tables rather than in objects, and so
-- the limits of a schema can be demonstrated rather than asserted —
-- see er-model-limitations.md.
--
-- Portable SQL. Verified by executing this file unchanged on SQLite 3 and
-- PostgreSQL 16.
-- =====================================================================


-- ---------------------------------------------------------------------
-- portfolio — an owner's cash balance and, through holding, their stock
-- ---------------------------------------------------------------------
CREATE TABLE portfolio (
    portfolio_id  CHAR(36)       NOT NULL,
    owner         VARCHAR(100)   NOT NULL,
    balance       DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    created_at    TIMESTAMP      NOT NULL,

    CONSTRAINT pk_portfolio PRIMARY KEY (portfolio_id)
);


-- ---------------------------------------------------------------------
-- holding — everything a portfolio owns of one ticker
--
-- A row exists only while the portfolio owns at least one share of that
-- ticker. Selling a position down to zero deletes the row; buying the
-- ticker again inserts a new one.
-- ---------------------------------------------------------------------
CREATE TABLE holding (
    holding_id    CHAR(36)   NOT NULL,
    portfolio_id  CHAR(36)   NOT NULL,
    ticker        VARCHAR(5) NOT NULL,

    CONSTRAINT pk_holding PRIMARY KEY (holding_id),

    CONSTRAINT fk_holding_portfolio FOREIGN KEY (portfolio_id)
        REFERENCES portfolio (portfolio_id) ON DELETE CASCADE,

    -- This is what Map<Ticker, Holding> means: at most one holding per
    -- ticker per portfolio.
    CONSTRAINT uq_holding_ticker_per_portfolio UNIQUE (portfolio_id, ticker),

    -- The domain rule is the pattern ^[A-Z]{1,5}$. Portable SQL has no
    -- regular expressions, so this constraint only approximates it: right
    -- length, nothing lowercase. It still admits 'AA1'. The Ticker value
    -- object enforces the exact pattern, which is the point made at
    -- length in er-model-limitations.md — a CHECK approximates a rule
    -- that a value object states exactly.
    CONSTRAINT ck_holding_ticker_shape
        CHECK (LENGTH(ticker) BETWEEN 1 AND 5 AND ticker = UPPER(ticker))
);


-- ---------------------------------------------------------------------
-- lot — one purchase, at one price, at one moment
--
-- initial_shares never changes; remaining_shares shrinks as sales consume
-- the lot. A lot reduced to zero is deleted.
-- ---------------------------------------------------------------------
CREATE TABLE lot (
    lot_id            CHAR(36)       NOT NULL,
    holding_id        CHAR(36)       NOT NULL,
    initial_shares    INTEGER        NOT NULL,
    remaining_shares  INTEGER        NOT NULL,
    unit_price        DECIMAL(19, 2) NOT NULL,
    purchased_at      TIMESTAMP      NOT NULL,

    CONSTRAINT pk_lot PRIMARY KEY (lot_id),

    CONSTRAINT fk_lot_holding FOREIGN KEY (holding_id)
        REFERENCES holding (holding_id) ON DELETE CASCADE,

    CONSTRAINT ck_lot_initial_shares_positive
        CHECK (initial_shares > 0),

    CONSTRAINT ck_lot_remaining_shares_in_range
        CHECK (remaining_shares >= 0 AND remaining_shares <= initial_shares),

    CONSTRAINT ck_lot_unit_price_positive
        CHECK (unit_price > 0)
);


-- FIFO reads the lots of one holding oldest first. The index makes that
-- ordering cheap; it does NOT make it happen. Nothing in this schema
-- obliges a sale to consume the oldest lot — a query that forgets
-- ORDER BY purchased_at ASC produces a wrong cost basis and no error.
CREATE INDEX idx_lot_fifo ON lot (holding_id, purchased_at);


-- =====================================================================
-- What this schema cannot say
--
--   * Which lot a sale consumes first. FIFO is an ordering applied by
--     the program, not a constraint. See the index comment above.
--   * How proceeds, cost basis and profit are computed. A sale returns
--     them; nothing stores them, and no column derives them.
--   * That every change must go through the portfolio. UPDATE lot SET
--     remaining_shares = 0 is legal SQL that no constraint will refuse.
--   * That a rejected sale changes nothing. In the domain that follows
--     from validating before mutating; here it needs a transaction.
--
-- er-model-limitations.md works through each of these.
-- =====================================================================
