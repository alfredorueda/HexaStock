# Architecture learning program: executive overview

HexaStock is a practical teaching environment for Domain-Driven Design and ports and adapters. The program helps students and engineering teams explain business rules, trace dependencies, test behavior and evaluate changes using evidence from a working Java application.

## Audience and entry point

Participants should know basic Java, unit tests and HTTP. They can begin with domain exercises without Docker or external API keys. [Start Here](../learning/START-HERE.md) provides a gentler route for students encountering the architecture for the first time.

## What participants produce

- A traced sale, with business rules distinguished from orchestration and persistence.
- Tests linked to concrete acceptance examples.
- A small extension with an explicit port contract and verification evidence.
- A short design decision explaining benefits, costs and limits.

The sequence starts with one domain rule, then crosses HTTP and persistence boundaries. Concurrency and reporting projections come later. Instructors can select sessions independently once prerequisites are met.

## What the repository demonstrates

The baseline includes FIFO trading, a rich domain model, JPA and MongoDB adapters, quote providers, behavioral specifications, OpenAPI documentation, unit and integration tests, and seven architecture checks. OpenAPI is maintained alongside code; automatic contract-first generation is not configured.

This is an educational reference, not evidence of production certification or a guaranteed organizational return. Production adoption would need additional decisions about authorization, data migrations, idempotency, operations and deployment. Proposed exercises such as watchlists and gRPC are not existing capabilities.

## Assessing learning

Evaluate whether participants can explain an invariant, locate its implementation, select a meaningful test and justify a boundary. A passing build supports that assessment but does not replace it. Team outcomes such as onboarding time or change failure rate require local measurement; they are not guaranteed by an architectural label.

See [the program and session deliverables](ARCHITECTURE-TRANSFORMATION-PROGRAM.md) for a concrete teaching sequence.
