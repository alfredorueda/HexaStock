# 3. Maintain evidence across changes

**Goal:** keep requirements, implementation, and verification aligned over time. Allow 25–35 minutes.

A living specification is maintained with the code it describes. It can be executable, such as an automated contract test, or reviewed against executable evidence, such as a diagram checked against source. Co-location and version control help; they do not detect semantic drift by themselves.

For each requirement change, review four things together: the behavior delta, affected contracts, implementation diff, and verification evidence. If a test fails, decide whether the requirement changed or the implementation is wrong before updating expected values. Automatically making every test match the new code can erase the very regression the test detected.

## A repeatable classroom experiment

Propose a maximum deposit amount as an extension; do not present it as existing behavior. Specify the currency, inclusivity of the boundary, and error response before implementing it in your own exercise copy.

| Stage | Evidence to retain |
|---|---|
| Baseline | Commit ID, environment, existing relevant test results |
| Requirement | Examples immediately below, at, and above the limit |
| Implementation | Reviewed diff and explanation of rule ownership |
| Verification | Independent expected values, focused tests, HTTP contract checks |
| Evolution | A second requirement change and the artifacts it affects |

Record tool/model versions if evaluating tools, inputs supplied, elapsed review time, defects found, and manual corrections. These measurements support a bounded comparison; a successful demonstration alone does not establish a general productivity gain or prove one model superior.

## Further experiments

Choose one question per experiment, with a human-reviewed reference result:

* **Traceability:** after changing a deposit rule, which tests and contracts need updating? Measure missed and incorrectly suggested locations.
* **Drift detection:** deliberately alter FIFO in a disposable exercise copy. Determine whether tests detect the change and whether an assistant accurately explains it.
* **Specification bootstrap:** draft behavior descriptions from existing code, then compare them with domain requirements. Reproducing a code defect in prose is not successful requirements discovery.
* **Repeated evolution:** follow three small changes and count stale artifacts after each. Portfolio reporting already exists; watchlists and dividend tracking would be new capabilities.
* **Stakeholder review:** give a domain expert a behavior diff and concrete examples. Record unresolved questions before implementation.
* **Behavior-preserving refactoring:** move responsibilities without changing contracts, then check both outputs and dependency rules. Introducing observable domain events may be a feature change rather than a pure refactoring.

## Evaluate workflow tools with the same discipline

OpenSpec and GitHub Spec Kit were proposed in the original chapter as candidates for evaluation; this repository does not establish their adoption or effectiveness. If evaluating any such tool, inspect its current official documentation and choose one small change first. Compare how it records decisions, updates existing Gherkin/OpenAPI artifacts, exposes uncertain assumptions, and supports review. Do not assume it can automatically reconcile semantic contradictions.

No particular product is required for this lesson. A versioned requirement, focused tests, a reviewable diff, and a clear record of decisions are enough to run the experiment. Models, context selection, tool access, specification quality, and reviewer expertise can all affect results.

**Checkpoint:** name one finding that would count against the approach. Examples include more review time than the baseline, missed boundary cases, or accumulating stale contracts despite successful generation.

Return to [the chapter guide](SPECIFICATION-DRIVEN-DEVELOPMENT-WITH-AI.md) or continue with [the lot-policy exercise](../DDD-Hexagonal-exercise.md).
