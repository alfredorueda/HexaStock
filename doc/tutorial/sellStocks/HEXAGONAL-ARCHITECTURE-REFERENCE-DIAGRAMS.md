# Hexagonal Architecture — Reference Sources and Diagrams

**Companion to [Sell Stock Tutorial](SELL-STOCK-TUTORIAL.md)**

---

This companion document presents the original source of Hexagonal Architecture together with two widely cited community diagrams that help visualise and teach the architectural style. For the HexaStock-specific architecture diagram showing the project's actual components, see the main tutorial.

---

## The Original Source: Alistair Cockburn

Hexagonal Architecture — also known as **Ports and Adapters** — was introduced by Alistair Cockburn in his original 2005 article. That article defines the core structural idea: an application core surrounded by ports (technology-neutral boundaries) and adapters (technology-specific implementations), with all dependencies pointing inward toward the domain. Every subsequent interpretation of this architecture — including the community diagrams reproduced below — builds upon the concepts articulated in that article.

This tutorial and the HexaStock codebase treat Cockburn's article as the primary conceptual source and historical foundation for the architectural style they describe. The community diagrams that follow are later explanatory interpretations that help visualise and teach the idea; they do not replace the original work.

> **Primary source**
>
> Cockburn, Alistair. "Hexagonal Architecture (Ports and Adapters)." *alistair.cockburn.us*, 2005.
> https://alistair.cockburn.us/hexagonal-architecture/

---

## Community Diagram: Simplified Teaching View (Tom Hombergs)

<img width="1600" height="797" alt="Hexagonal Architecture" src="https://github.com/user-attachments/assets/09c46496-b801-4375-801a-aebe1361d57d" />

> *Image credit:*
> *Diagram by **Tom Hombergs**, reproduced for educational purposes with proper attribution.*
> *Sources:*
> *– Article: [Hexagonal Architecture with Java and Spring](https://reflectoring.io/spring-hexagonal/)*
> *– Reference implementation: [BuckPal – A Hexagonal Architecture Example](https://github.com/thombergs/buckpal)*

This widely cited diagram presents Cockburn's Ports and Adapters idea in a simplified visual form suited to teaching. Driving (primary) adapters on the left send requests into the application core through inbound ports; driven (secondary) adapters on the right implement outbound ports that the core uses to interact with external systems. All dependencies point inward toward the domain — the central rule established by Cockburn's original article.

---

## Community Diagram: Expanded Architectural Interpretation (Herberto Graça)

<img width="876" height="657" alt="image" src="https://github.com/user-attachments/assets/91ddf125-0949-4251-87ac-ab0856698376" />

> *Image credit:*
> *Diagram by **Herberto Graça**, reproduced for educational purposes with proper attribution.*
> *Source: [Explicit Architecture #01: DDD, Hexagonal, Onion, Clean, CQRS, … How I put it all together](https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/)*

This diagram extends Cockburn's foundational model into a richer environment where multiple inbound and outbound ports coexist and where architectural styles such as DDD, Hexagonal, Onion, and Clean Architecture are combined into a single coherent view. It is a valuable pedagogical resource for understanding how the original Ports and Adapters idea scales to more complex systems.

---

## Foundational and Community References

1. **Cockburn, Alistair.** "Hexagonal Architecture (Ports and Adapters)." *alistair.cockburn.us*, 2005. https://alistair.cockburn.us/hexagonal-architecture/ — **The original article that introduced the architectural style.**
2. **Hombergs, Tom.** *Get Your Hands Dirty on Clean Architecture.* Packt Publishing, 2019. Reference implementation: [BuckPal](https://github.com/thombergs/buckpal).
3. **Graça, Herberto.** "DDD, Hexagonal, Onion, Clean, CQRS, … How I Put It All Together." *herbertograca.com*, 2017. https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/
