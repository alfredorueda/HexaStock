# Hexagonal Architecture — Reference Diagrams

**Companion to [Sell Stock Tutorial](SELL-STOCK-TUTORIAL.md)**

---

This document collects two widely referenced hexagonal architecture diagrams from the community. They illustrate the conceptual foundation and a more complete architectural composition. For the HexaStock-specific architecture diagram showing the project's actual components, see the main tutorial.

---

## Simplified Hexagonal Architecture (Tom Hombergs)

<img width="1600" height="797" alt="Hexagonal Architecture" src="https://github.com/user-attachments/assets/09c46496-b801-4375-801a-aebe1361d57d" />

> *Image credit:*  
> *The architectural diagram referenced in this tutorial is based on work by **Tom Hombergs**.*  
> *Sources:*  
> *– Article: [Hexagonal Architecture with Java and Spring](https://reflectoring.io/spring-hexagonal/)*  
> *– Reference implementation: [BuckPal – A Hexagonal Architecture Example](https://github.com/thombergs/buckpal)*  
> *Used for educational purposes with proper attribution.*

This diagram illustrates the core idea of Hexagonal Architecture in a simplified form: a domain-centered system surrounded by ports and adapters. Driving (primary) adapters on the left send requests into the application core through inbound ports; driven (secondary) adapters on the right implement outbound ports that the core uses to interact with external systems. All dependencies point inward toward the domain.

---

## Explicit Architecture (Herberto Graça)

<img width="876" height="657" alt="image" src="https://github.com/user-attachments/assets/91ddf125-0949-4251-87ac-ab0856698376" />

> *Image credit:*  
> *The architectural diagram referenced in this tutorial is based on work by **Herberto Graça**.*  
> *Source: [Explicit Architecture #01: DDD, Hexagonal, Onion, Clean, CQRS, … How I put it all together](https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/)*  
> *Used for educational purposes with proper attribution.*

This diagram provides a more detailed architectural view. It shows how the same principles apply in a richer environment where multiple inbound and outbound ports coexist, and where architectural styles such as DDD, Hexagonal, Onion, and Clean Architecture can be combined. Together with the Hombergs diagram above, it illustrates both the conceptual foundation and a more complete architectural composition.

---

## Further Reading

- Cockburn, Alistair. "Hexagonal Architecture (Ports and Adapters)." *alistair.cockburn.us*, 2005. https://alistair.cockburn.us/hexagonal-architecture/
- Hombergs, Tom. *Get Your Hands Dirty on Clean Architecture.* Packt Publishing, 2019. Reference implementation: [BuckPal](https://github.com/thombergs/buckpal).
- Graça, Herberto. "DDD, Hexagonal, Onion, Clean, CQRS, … How I Put It All Together." *herbertograca.com*, 2017. https://herbertograca.com/2017/11/16/explicit-architecture-01-ddd-hexagonal-onion-clean-cqrs-how-i-put-it-all-together/
