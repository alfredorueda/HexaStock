# Error contract

How a failed operation reports itself.

Every failure is a **domain exception** — this is a domain model, with no HTTP layer to return
a status code. The last column records the HTTP status a later API layer should map each
exception to, via an RFC 7807 problem detail, so that layer stays consistent when it arrives.
Nothing in this project enforces it.

| Exception                   | Domain meaning                                       | Typical message                                             | HTTP status (later API layer)  |
| --------------------------- | ---------------------------------------------------- | ----------------------------------------------------------- | ------------------------------ |
| `InvalidQuantityException`  | The quantity is zero, negative, or otherwise invalid | `Quantity must be positive: <value>`                         | 400 Bad Request                |
| `InvalidAmountException`    | A monetary amount (here, the price) is not positive  | `Price must be positive: <value>`                            | 400 Bad Request                |
| `ConflictQuantityException` | Not enough shares to satisfy the sale                | `Not enough shares to sell. Available: <n>, Requested: <m>`  | 409 Conflict                   |
| `InvalidTickerException`    | The ticker is not 1–5 uppercase letters              | `Invalid ticker: <value>` / `Ticker cannot be empty`         | 400 Bad Request                |
| `HoldingNotFoundException`  | The portfolio does not hold the requested ticker     | `Holding not found in portfolio: <ticker>`                   | 404 Not Found                  |

The exception messages matter beyond the domain: in RFC 7807 the `detail` field of the error
response is taken from the exception message, so keeping these strings exact means a future API
layer already produces the right payload. The tests assert on them.

## Not implemented here

| Exception                    | Why not                                                                                       |
| ---------------------------- | --------------------------------------------------------------------------------------------- |
| `PortfolioNotFoundException` | Raised by whatever looks a portfolio up by id. A domain-only kata has no repository, so there is no place for it to be thrown. |

## Which error wins when two preconditions are violated at once

Order matters, because it decides which failure the caller sees. `Portfolio.sell` validates the
**quantity before it looks up the holding**, so selling zero shares of a ticker that is not held
is an invalid-quantity failure, not a missing-holding one. This is pinned down by AC-15 in
[`sell-stocks-spec.md`](sell-stocks-spec.md).
