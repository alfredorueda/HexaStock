---
applyTo: "src/test/java/**/*.java"
---

- Use JUnit 5.
- Make AC-01 through AC-24 explicit in test names or display names.
- Use the exact examples in the behaviour specification.
- Assert remaining lots, quantities, cash balance, lifecycle, and unchanged state after rejection.
- Compare `BigDecimal` values by numeric value so scale alone does not fail a test.
- Do not derive expected values from the implementation under test.
