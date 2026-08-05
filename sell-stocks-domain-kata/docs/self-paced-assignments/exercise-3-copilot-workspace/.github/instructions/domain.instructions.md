---
applyTo: "src/main/java/**/*.java"
---

- Match the classes, fields, methods, visibility, and relationships in
  `docs/spec/domain-class-diagram.puml`.
- Implement only behaviour required by `docs/spec/sell-stocks-spec.md`.
- Keep the domain free of framework and infrastructure imports.
- Use immutable validated value objects where the diagram requires them.
- Validate the whole operation before mutation when the specification requires all-or-nothing
  behaviour.
- Use `BigDecimal` for money and preserve the specified rounding policy.
