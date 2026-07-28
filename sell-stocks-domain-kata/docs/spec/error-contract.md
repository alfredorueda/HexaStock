# Error contract

How a failed operation reports itself.

This kata is domain-only, so every failure is a **domain exception** — there is no HTTP layer
to return a status code. The full system maps each exception to an HTTP status via an RFC 7807
problem detail; that mapping is recorded in the last column purely for traceability, and is
what a later API layer would implement. Nothing in this project enforces it.

| Exception                   | Domain meaning                                       | Typical message                                             | HTTP status in the full system |
| --------------------------- | ---------------------------------------------------- | ----------------------------------------------------------- | ------------------------------ |
| `InvalidQuantityException`  | The quantity is zero, negative, or otherwise invalid | `Quantity must be positive: <value>`                         | 400 Bad Request                |
| `InvalidAmountException`    | A monetary amount (here, the price) is not positive  | `Price must be positive: <value>`                            | 400 Bad Request                |
| `ConflictQuantityException` | Not enough shares to satisfy the sale                | `Not enough shares to sell. Available: <n>, Requested: <m>`  | 409 Conflict                   |
| `HoldingNotFoundException`  | The portfolio does not hold the requested ticker     | `Holding not found in portfolio: <ticker>`                   | 404 Not Found                  |

The exception messages matter beyond the domain: in RFC 7807 the `detail` field of the error
response is taken from the exception message, so keeping these strings exact means a future API
layer already produces the right payload. The tests assert on them.

## Not implemented here

| Exception                    | Why not                                                                                       |
| ---------------------------- | --------------------------------------------------------------------------------------------- |
| `PortfolioNotFoundException` | Raised by whatever looks a portfolio up by id. A domain-only kata has no repository, so there is no place for it to be thrown. |
| `InvalidTickerException`     | Malformed tickers are an open question — the spec defines no outcome for one. See §6.1 of [`sell-stocks-spec.md`](sell-stocks-spec.md). |

## Which error wins when two preconditions are violated at once

Order matters, because it decides which failure the caller sees. `Portfolio.sell` validates the
**quantity before it looks up the holding**, so selling zero shares of a ticker that is not held
is an invalid-quantity failure, not a missing-holding one. This is pinned down by AC-15 in
[`sell-stocks-spec.md`](sell-stocks-spec.md).
