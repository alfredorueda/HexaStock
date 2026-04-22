# Domain-Driven Design, Hexagonal Architecture, Onion Architecture, and Clean Architecture: A Comparative and Integrative Study

*A reference essay illustrated with the HexaStock reference project*

## Abstract

This essay offers a rigorous comparative analysis of four influential bodies of work in modern software design: Domain-Driven Design (DDD), Hexagonal Architecture (Ports and Adapters), Onion Architecture, and Clean Architecture. Although these approaches are routinely mentioned together — and often conflated — they belong to different conceptual categories and were proposed in response to different concerns. DDD is, primarily, a methodology for modeling complex business domains; Hexagonal, Onion, and Clean Architecture are architectural styles that organize source code around a protected domain core and an inverted dependency rule. The article explains the historical motivation of each approach, dissects their core principles, examines their similarities and differences, and discusses how they relate to and reinforce one another in practice. Throughout the discussion, the HexaStock reference project — a Java 21 / Spring Boot 3 backend implementing a personal investment portfolio domain — is used as a concrete illustration of how these ideas materialize in working code. The conclusion argues that, in mature engineering practice, these approaches are best understood not as competitors but as overlapping families of ideas that can be combined judiciously to manage essential complexity in long-lived business systems.

## 1. Introduction

Few topics in software architecture generate as much enthusiasm — and as much confusion — as the cluster of ideas surrounding Domain-Driven Design and the so-called "clean" family of architectures. Practitioners routinely speak of *"a hexagonal project with DDD"*, *"clean architecture with ports and adapters"*, or *"onion-style domain modeling"*, frequently treating these labels as interchangeable. They are not. Each label originated in a distinct intellectual context, with a distinct primary concern, and addresses a distinct subset of the problems that arise when building software for complex, evolving business domains.

The aim of this essay is to clarify these distinctions without exaggerating them. The four approaches under examination overlap substantially, and in well-designed systems they tend to coexist rather than collide. Our goal is to articulate, with academic rigor and practical concreteness, how each one contributes to a coherent design discipline, and how a software team — building, for example, a system such as HexaStock — can profit from understanding their relationships rather than choosing one as an exclusive doctrine.

## 2. Historical and Conceptual Origins

Domain-Driven Design was introduced by Eric Evans in his 2003 book *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Evans's work is best read as a synthesis: it consolidates a generation of object-oriented modeling practice and extends it with strategic patterns — *bounded contexts*, *context maps*, *ubiquitous language* — alongside tactical building blocks such as *entities*, *value objects*, *aggregates*, *repositories*, and *domain services*. DDD was never proposed as an architectural style in the structural sense; it is a methodology for *modeling*, with strong opinions about how that model should be expressed in code, and how teams should organize themselves around it.

Hexagonal Architecture was articulated by Alistair Cockburn in 2005 (with earlier drafts circulating since the late 1990s) under the alternative name *Ports and Adapters*. Cockburn's central concern was symmetric isolation of the application from its environment: the same application logic should be drivable by tests, by a user interface, by a message queue, or by a scheduled job, without altering its core. The hexagonal metaphor is geometric rather than hierarchical — there is no top or bottom, only an inside and an outside, separated by *ports* (abstract interfaces owned by the application) and *adapters* (technology-specific implementations).

Onion Architecture was proposed by Jeffrey Palermo between 2008 and 2009 as a response to the recurring degradation of layered enterprise applications, in which the domain layer ended up depending on persistence frameworks, ORMs, and even UI concerns. Palermo's contribution was to draw the layered diagram as a set of concentric rings, with the domain model at the center, and to formalize the rule that *all dependencies point inward*. Outer rings know about inner rings; inner rings know nothing about outer rings.

Clean Architecture was popularized by Robert C. Martin in a series of blog posts (2012) and consolidated in his 2017 book *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Martin explicitly acknowledges that his proposal is a synthesis of pre-existing styles, including Hexagonal, Onion, *Boundary–Control–Entity*, *Screaming Architecture*, and *DCI*. Its central contribution is a clear formulation of the *Dependency Rule* — source code dependencies may only point inward — together with a vocabulary of concentric layers (*Entities*, *Use Cases*, *Interface Adapters*, *Frameworks and Drivers*) and an emphasis on the separation between *policy* and *mechanism*.

These four approaches are therefore not contemporaneous and not equivalent in scope. DDD is a modeling discipline; Hexagonal, Onion, and Clean Architecture are architectural styles whose differences are largely matters of emphasis and metaphor. Recognizing this asymmetry is the first step toward using them coherently.

## 3. Core Principles of Domain-Driven Design

DDD rests on the conviction that the most enduring source of complexity in business software is the business itself. Frameworks change, persistence technologies change, user interfaces change; the rules governing how a portfolio accrues realized profit when a stock lot is sold under FIFO accounting do not. DDD therefore places the *domain model* at the center of the design effort and demands that this model be expressed in a *ubiquitous language* shared by domain experts and developers alike.

Strategically, DDD distinguishes *bounded contexts* — explicit linguistic and model boundaries within which a particular interpretation of the domain holds — and provides patterns (*context map*, *anti-corruption layer*, *shared kernel*, *customer-supplier*) for the relationships between them. Tactically, it offers a vocabulary of building blocks: *entities* (objects with identity and lifecycle), *value objects* (immutable, identity-less, defined by their attributes), *aggregates* (clusters of entities and value objects governed by a single root that enforces invariants), *repositories* (collection-like abstractions for retrieving aggregates), *domain services* (operations that do not naturally belong to a single entity), *domain events* (facts about something that has happened in the domain), and *factories*.

A subtle but essential point is that DDD is not, by itself, an architectural pattern in the structural sense. It does not prescribe a directory layout, a dependency direction, or a deployment topology. It describes *what* the model should look like and *why*; the choice of *how* to host that model in a runnable system is delegated to an architectural style — typically Hexagonal, Onion, or Clean.

In HexaStock, the domain module embodies these ideas concretely. The `Portfolio` aggregate root enforces invariants such as sufficient cash for a purchase and sufficient holdings for a sale; `Money`, `Ticker`, and `Quantity` appear as value objects; `Lot` represents an immutable purchase record consumed by FIFO sales; `Holding` aggregates lots for a single ticker and is responsible for cost-basis arithmetic. A `StockPriceProvider` is expressed as a domain-level abstraction (a port) rather than as a dependency on Finnhub or AlphaVantage. The ubiquitous language is preserved across artifacts: the words *portfolio*, *holding*, *lot*, *deposit*, *purchase*, *sale*, *realized gain*, and *unrealized gain* mean the same thing in domain code, REST DTOs, OpenAPI specifications, and conversations with stakeholders.

## 4. Core Principles of Hexagonal Architecture

Hexagonal Architecture organizes a system around a central application that communicates with the outside world exclusively through *ports*. A port is an interface, defined and owned by the application, that expresses an interaction in terms meaningful to the domain. *Driving* (or *primary*) ports describe what the application offers to its environment — for example, *open a portfolio*, *register a deposit*, *buy a quantity of a ticker*. *Driven* (or *secondary*) ports describe what the application requires from its environment — for example, *persist a portfolio*, *load a portfolio by identifier*, *obtain the current price of a ticker*.

Adapters are concrete implementations of ports. *Inbound* adapters translate external stimuli (HTTP requests, message events, scheduled triggers, CLI invocations) into calls on driving ports. *Outbound* adapters implement driven ports against specific technologies (a JPA repository, a MongoDB repository, an HTTP client to a market data provider). The core of the hexagon — application services and domain model — has no compile-time knowledge of any adapter; it depends only on the abstractions it itself defines.

The pedagogical force of the hexagonal metaphor lies in its symmetry. There is no privileged direction: the database is not "below" the application, the UI is not "above" it. All collaborators are simply *outside*, behind ports. This symmetry has two important consequences. First, the application becomes trivially testable in isolation: tests are simply another inbound adapter, and outbound dependencies can be replaced by in-memory implementations of the same ports. Second, technological substitutions become local refactorings: replacing JPA with MongoDB, or Finnhub with AlphaVantage, affects only the corresponding adapter module.

HexaStock displays this organization explicitly in its module layout. The `application` module declares ports; `adapters-inbound-rest` translates HTTP into use-case invocations; `adapters-outbound-persistence-jpa` and `adapters-outbound-persistence-mongodb` provide alternative implementations of the persistence ports; `adapters-outbound-market` implements the price port using Finnhub primarily and AlphaVantage as fallback. The very name of the project is a deliberate declaration of architectural intent.

## 5. Core Principles of Onion Architecture

Onion Architecture preserves the spirit of Hexagonal but reintroduces an explicitly hierarchical, concentric vocabulary. At the center sits the *domain model*: entities, value objects, and domain services expressed in pure language with no dependencies on infrastructure. Surrounding it is the *domain services* ring (when distinguished from the model ring), then the *application services* ring containing use-case orchestration, and finally the outermost ring of *infrastructure*, *user interface*, and *tests*.

The defining rule of Onion Architecture is that source code dependencies always point inward. Outer rings may reference inner rings; inner rings must remain ignorant of outer rings. Where Hexagonal Architecture emphasizes the *boundary* and the *symmetry* of interactions through ports, Onion Architecture emphasizes the *layering* of conceptual responsibilities and the *protection* of the domain through inversion of control. In practice, the two styles converge: an honest implementation of either ends up with a domain core, application services that orchestrate it, abstractions for external collaborators, and adapter implementations at the edges.

Onion Architecture's particular contribution is rhetorical and pedagogical. By drawing the system as concentric rings with the domain at the literal center, it makes the architectural priority unmistakable: *the domain is the reason the software exists*; everything else is scaffolding that should not be allowed to leak into the model.

## 6. Core Principles of Clean Architecture

Clean Architecture is, by Martin's own admission, a synthesis. Its concentric diagram identifies four typical layers — *Entities* (enterprise-wide business rules), *Use Cases* (application-specific business rules), *Interface Adapters* (controllers, presenters, gateways), and *Frameworks and Drivers* (web frameworks, databases, devices) — but the number and naming of layers are explicitly negotiable. What is non-negotiable is the *Dependency Rule*: source code dependencies may only point inward, toward higher-level policy.

Clean Architecture also generalizes a distinction that pervades the other styles but is not always made explicit: the separation between *policy* (decisions that are valuable independently of how they are realized) and *mechanism* (the technical means by which decisions are carried out). A use case is a policy; the HTTP framework that exposes it is a mechanism. An aggregate's invariants are policy; the relational schema that stores them is a mechanism. Architectural quality is measured, in this view, by how cleanly policy is insulated from changes in mechanism.

A further contribution of Clean Architecture is its emphasis on *use cases* as first-class artifacts. Where Hexagonal speaks of driving ports and Onion speaks of application services, Clean Architecture elevates the use case to the central organizing unit of the application layer, often realized as a class per use case with explicit input and output boundary objects. This is a stylistic preference rather than a structural necessity, but it has proven influential.

## 7. Comparative Analysis

### 7.1 Goals

DDD seeks to *manage essential domain complexity* through faithful modeling and shared language. Hexagonal Architecture seeks *isolation from technological context* through symmetric ports and adapters. Onion Architecture seeks *protection of the domain* through concentric layering and dependency inversion. Clean Architecture seeks *independence of policy from mechanism* through a generalized inward dependency rule and explicit use cases. These goals are compatible; they emphasize different facets of the same broader concern.

### 7.2 Dependency Rules

All three architectural styles converge on the principle that infrastructure depends on the domain, not the other way around. The mechanism of inversion is the same in each: define abstractions where they are *used* (in the inner layers) and implement them where the technology lives (in the outer layers). DDD does not, strictly speaking, prescribe a dependency rule, but its repository pattern presupposes one: the repository interface belongs to the domain or application layer; its implementation belongs to infrastructure.

### 7.3 Treatment of the Domain Model

All four approaches insist that the domain model be expressed in a language faithful to the business and free from infrastructural contamination. DDD goes furthest in prescribing *how* the model should be designed (entities, value objects, aggregates, invariants). The architectural styles are largely agnostic about the internal shape of the model; they are concerned with where it sits and what it is allowed to depend on.

### 7.4 Handling of Infrastructure

Hexagonal frames infrastructure as adapters behind ports. Onion frames it as the outermost ring. Clean Architecture frames it as *Frameworks and Drivers*. The substantive content is identical: persistence, messaging, web frameworks, and external services are pushed to the periphery, accessed through abstractions owned by the inner layers. In HexaStock, this is visible in the explicit separation between the `application` module (where `PortfolioRepository` and `StockPriceProvider` are declared) and the `adapters-outbound-*` modules (where they are implemented for JPA, MongoDB, Finnhub, and AlphaVantage).

### 7.5 Role of Use Cases and Application Services

Clean Architecture promotes the use case to a first-class structural element. Hexagonal Architecture treats the same concept as a driving port and its implementing application service. Onion Architecture places application services in a dedicated ring around the domain. DDD discusses *application services* as thin orchestrators that coordinate domain objects without containing business logic. The terminology differs; the responsibility is the same: translate an external intention (*"deposit one thousand euros into portfolio X"*) into a coherent invocation of domain operations within a transactional boundary.

### 7.6 Testability

All three architectural styles, when honestly implemented, yield highly testable systems. The domain model can be exercised in pure unit tests with no framework involvement; application services can be tested with in-memory implementations of outbound ports; adapters can be tested in isolation against their respective technologies. Hexagonal Architecture is perhaps the most explicit on this point, since testability is one of Cockburn's original motivations.

### 7.7 Adaptability to Frameworks

A frequent misunderstanding is that these architectures are anti-framework. They are not; they are *anti-framework-driven-design*. The intent is that frameworks such as Spring Boot, Hibernate, or Jakarta EE be treated as adapters — powerful, useful, and confined to the outer layers — rather than as the organizing principle of the system. HexaStock uses Spring Boot extensively, but Spring annotations appear in adapter and bootstrap modules, not in the domain model. The domain remains a plain Java module that could, in principle, be reused under any framework.

### 7.8 Suitability for Complex Business Systems

For systems whose value lies in the faithfulness of their business logic — financial systems, order management, insurance underwriting, logistics, healthcare — the combined discipline of DDD plus any of the three architectural styles is well established as the most sustainable approach. For simple CRUD systems with little behavioral complexity, the overhead of these patterns is rarely justified. Architectural maturity consists, in part, in recognizing which category a given system belongs to.

## 8. Relationship with DDD

The most important clarification this essay can offer is that DDD is not in competition with Hexagonal, Onion, or Clean Architecture. DDD answers the question *"how should we model the domain?"*; the three architectural styles answer the question *"where in the codebase should the model live, and what is it allowed to depend on?"*. A team practicing DDD will inevitably need an architectural style to host its model, and any of the three will serve.

Hexagonal Architecture supports DDD by giving repositories, domain event publishers, and external service abstractions a natural home as outbound ports. Onion Architecture supports DDD by giving the aggregate-centered domain model the literal and figurative center of the system. Clean Architecture supports DDD by formalizing use cases as application-layer policies that orchestrate aggregates without contaminating them.

A common misunderstanding is to equate "having a `domain` package" with "doing DDD". DDD is a modeling discipline whose presence is judged by the *quality* of the model — the clarity of the ubiquitous language, the precision of aggregate boundaries, the integrity of invariants, the alignment between code and domain experts' speech — not by the presence of a folder named *domain*. Conversely, a team can practice DDD competently without ever using the words *hexagonal*, *onion*, or *clean*, provided that the model is properly insulated from infrastructural concerns.

A second common misunderstanding is to identify DDD with the use of object-relational mapping in a particular way, or with the existence of a `@Repository` annotation. The repository pattern in DDD is a conceptual abstraction over a collection of aggregates; whether it is implemented with JPA, MongoDB, an event store, or in-memory data structures is an infrastructural detail. HexaStock illustrates this neatly by providing two interchangeable implementations of the same `PortfolioRepository` port — one based on JPA, one on MongoDB — without any change to the domain or application modules.

## 9. Advantages and Disadvantages

DDD offers, at its best, a sustainable way to manage essential complexity, a shared language that reduces translation errors between domain experts and developers, and a tactical vocabulary that makes design decisions explicit and reviewable. Its costs are non-trivial: it presupposes access to domain experts, demands a substantial learning curve, and tends to be over-applied in domains that do not warrant it. Practiced as ritual rather than as discipline, it produces ceremonious code with no real modeling content.

Hexagonal Architecture offers symmetric isolation, excellent testability, and a clean answer to the recurring question *"where does this technology go?"*. Its principal cost is the proliferation of interfaces and the indirection they impose: every external interaction acquires a port, an adapter, and a wiring step. In small systems, this overhead can outweigh its benefits.

Onion Architecture offers a clear pedagogical metaphor and a strong rhetorical defense of the domain. Its disadvantages are largely shared with Hexagonal: indirection, ceremony, and the temptation to introduce layers for their own sake. Compared to Hexagonal, it is sometimes criticized for its hierarchical bias, which can encourage a subtle return to the layered thinking it originally sought to discipline.

Clean Architecture offers the most explicit and generalizable formulation of the dependency rule, together with a useful emphasis on use cases as first-class artifacts. Its costs are the heaviest of the three: a strict reading produces a substantial number of classes per use case (input boundary, output boundary, interactor, presenter, view model), which can feel disproportionate in systems where simpler structures would suffice.

Across all three styles, the dominant trade-off is the same: *insulation has a price, and that price is indirection*. The discipline of architectural maturity consists in paying this price where it is justified by long-term maintainability and refusing to pay it where it is not.

## 10. Practical Guidance for Software Teams

Several practical observations follow from the preceding analysis.

First, a team should choose its architectural vocabulary primarily for communicative reasons. If the team is already fluent in *ports and adapters*, calling the system hexagonal is appropriate; if it thinks in concentric layers, *onion* may be clearer; if it prefers Martin's terminology, *clean* will resonate. The structural consequences are largely the same.

Second, conceptual architecture and package or module structure are related but not identical. A project may have a single Maven module and still respect the dependency rule through disciplined package organization; conversely, a multi-module project with a tidy hexagonal layout may still leak infrastructure into the domain through careless dependencies. HexaStock chooses to make architecture visible at the module level — `domain`, `application`, `adapters-inbound-rest`, `adapters-outbound-persistence-jpa`, `adapters-outbound-persistence-mongodb`, `adapters-outbound-market`, `bootstrap` — precisely to make violations of the dependency rule fail at compile time rather than at code review.

Third, frameworks must be treated as adapters, not as design centers. Spring Boot is an exceptional platform; it is also a powerful gravitational field that can pull design decisions toward annotation-driven configuration, transactional proxies, and entity classes that double as domain objects. Architectural discipline consists in resisting this pull where it would compromise the integrity of the model. In HexaStock, the `domain` module has no Spring dependencies whatsoever; Spring lives in the adapter and bootstrap modules, where it belongs.

Fourth, real codebases are hybrid. They borrow vocabulary from several sources, deviate from textbook diagrams under legitimate pressure, and accumulate compromises over time. A mature team treats its architecture as a living constraint to be revisited, not as a sacred geometry to be defended. The value of Hexagonal, Onion, and Clean Architecture is that they provide a shared baseline against which deviations can be discussed honestly.

Fifth, protecting the domain from infrastructure matters because *the domain is what survives*. Persistence technologies, message brokers, web frameworks, cloud providers, and external APIs change on timescales of years; the rules of the business change on timescales of decades. A system whose business rules are entangled with its persistence layer pays for that entanglement every time either evolves. A system that has paid the price of separation upfront enjoys, for the rest of its life, the freedom to evolve its periphery without endangering its core.

## 11. Conclusion

Domain-Driven Design, Hexagonal Architecture, Onion Architecture, and Clean Architecture are best understood as members of the same intellectual family rather than as competing doctrines. They share a common conviction — that complex business software must be organized around a faithful, well-protected domain model — and differ mainly in vocabulary, metaphor, and emphasis. DDD contributes a modeling discipline; Hexagonal contributes a symmetric boundary metaphor; Onion contributes a concentric layering metaphor; Clean Architecture contributes a generalized dependency rule and a use-case-centric application layer.

In practice, mature teams combine these contributions freely. They model their domain in the spirit of DDD; they organize their code in the spirit of Hexagonal, Onion, or Clean Architecture (often blending all three); they treat frameworks as adapters; and they accept that their codebase will deviate, in a hundred small ways, from any textbook diagram. The HexaStock project is one concrete instance of this synthesis: a Spring Boot 3 / Java 21 backend in which a DDD-style domain (portfolios, holdings, lots, FIFO sales, realized and unrealized gains) lives at the center of an explicitly hexagonal module structure, with interchangeable persistence and market-data adapters demonstrating, in working code, the freedom that disciplined separation buys.

The deepest lesson of these four traditions is not architectural at all. It is methodological: that the long-term health of a software system depends more on the clarity of its model and the integrity of its boundaries than on any particular technological choice. Whichever vocabulary a team adopts, it is this underlying conviction — that the domain deserves protection because the domain is the reason the software exists — that ultimately determines whether the architecture endures.

## 12. Final Synthesis Table

| Dimension | DDD | Hexagonal Architecture | Onion Architecture | Clean Architecture |
|---|---|---|---|---|
| **Primary focus** | Modeling complex business domains | Isolating the application from external technologies | Protecting the domain through concentric layering | Separating policy from mechanism via dependency inversion |
| **Main metaphor** | Ubiquitous language and aggregates | Hexagon with ports and adapters | Concentric onion rings | Concentric layers with explicit use cases |
| **Dependency direction** | Implicit; repositories abstract infrastructure | From adapters inward to the application core | Strictly inward toward the domain | Strictly inward toward higher-level policy |
| **Treatment of infrastructure** | Hidden behind repository and service abstractions | Outbound adapters behind driven ports | Outermost ring, ignorant to the domain | *Frameworks and Drivers* layer at the periphery |
| **Role of the domain model** | Central, designed with tactical patterns (entities, value objects, aggregates) | Central application core | Literal center of the diagram | *Entities* layer at the innermost ring |
| **Relationship with use cases** | *Application services* as thin orchestrators | Driving ports implemented by application services | Application services ring around the domain | First-class *Use Cases* layer with explicit boundaries |
| **Relationship with DDD** | Is DDD itself; a modeling discipline | Hosts a DDD model naturally via ports | Places the DDD model at the center | Hosts a DDD model with explicit use-case orchestration |
| **Main strengths** | Sustainable management of essential complexity; shared language | Symmetric isolation; testability; technological substitutability | Clear pedagogical defense of the domain | Generalized dependency rule; explicit use cases |
| **Main limitations** | Heavy when misapplied to simple domains; demands domain expertise | Indirection and interface proliferation | Risk of subtle layered thinking; ceremony | Heaviest ceremony of the three in strict readings |

## 13. Recommended Interpretation for Modern Software Teams

A balanced contemporary stance is to treat Domain-Driven Design as the *modeling discipline* and Hexagonal, Onion, and Clean Architecture as overlapping *vocabularies for organizing the code that hosts the model*. Teams should resist the temptation to elevate any single label into a doctrine, recognize that real systems blend influences from all four, and judge architectural quality not by conformity to a diagram but by the clarity of the domain model, the integrity of its boundaries, and the freedom with which the periphery can evolve. The HexaStock project illustrates this stance concretely: a faithful domain at the center, ports declared by the application, multiple interchangeable adapters at the edges, frameworks confined to where they belong, and a module structure that makes the dependency rule a property of the build rather than an aspiration in the documentation. That, more than any particular name, is what these four traditions, taken together, are ultimately about.
