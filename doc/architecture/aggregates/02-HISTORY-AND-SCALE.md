# 2. Query History Separately

**Goal:** distinguish a useful aggregate boundary from a complete performance solution.

## Current state and growing history have different uses

A purchase or sale needs the current portfolio. A transaction-history screen needs past operations, possibly filtered and paginated. Keeping history outside the portfolio avoids making every business operation traverse that history.

This does not automatically bound the aggregate. Many purchases can create many active lots, and no configured maximum caps holdings or lots. Removing depleted lots reduces retained state; it does not establish a fixed upper bound.

The current `TransactionPort` exposes `getTransactionsByPortfolioId` and `save`. `TransactionService` retrieves the full list. Its optional `type` argument is currently ignored. Pagination, ticker/date filtering and cursor ordering are **proposed extensions**, not existing behavior.

## Measure before choosing a loading strategy

Database cost depends on the query, mapping, indexes, data distribution and persistence context. Avoid assuming that every lazy collection operation initializes the entire collection, or that cascading always updates every child row. Inspect the generated SQL for the actual operation.

A useful performance experiment records:

| Measurement | Why it matters |
|---|---|
| Holdings and active lots per portfolio | Defines the size of state being loaded. |
| SQL statements and returned rows | Distinguishes round trips from excessive result size. |
| Allocation and retained heap | Reveals object and mapping costs. |
| Response-time distribution | Shows typical and slow requests. |
| Concurrent writers per portfolio | Reveals lock waits or optimistic retry rates. |

The JPA entities currently use batch size 30 for collections. Batching can reduce round trips under suitable loading conditions; it is not a universal query-count bound. Domain objects are mapped separately from managed JPA entities.

## Design a paginated history extension

Start by specifying the public contract: allowed filters, maximum page size, ordering and behavior for an unknown portfolio. Then add a port method, adapter query and tests together.

For cursor pagination, use a deterministic pair such as `(createdAt, id)` rather than a timestamp alone. Two records can have the same timestamp. The next-page condition must agree with both the ordering and the cursor.

Offset pagination is straightforward and supports jumping to a page. Large offsets can require more work, and concurrent inserts can shift results. Cursor pagination can avoid revisiting preceding rows when a suitable index is available, but it does not guarantee constant response time or a stable historical snapshot under arbitrary edits.

Both approaches can query transaction records directly. A query does not have to reconstruct the aggregate merely because those records are associated with its ID.

## Do not confuse a record with a complete audit trail

`SaleTransaction` stores ticker, quantity, price, proceeds, profit and time. It does not store a per-lot allocation breakdown. Removing a depleted lot therefore means that exact original lot identity and allocation are not preserved by the sale record alone.

The domain records are immutable Java values. Database immutability is a separate concern: ordinary repository `save` operations and database access are not a tamper-proof or append-only storage policy. Stronger audit requirements would need an explicit retention model, access controls and tests.

Cash is maintained in the portfolio. Market value depends on external quotes and is calculated for reporting; persisting it would also require a freshness policy.

## Checkpoint

Does placing transactions in a separate table make the history endpoint efficient for a million records?

**Answer:** no. It permits a targeted query design, but the current full-list port still returns all matching records. Pagination, indexing and measurements are separate work.

[Previous: the boundary](01-CONSISTENCY-BOUNDARY.md) · [Next: alternatives](03-ALTERNATIVES.md)
