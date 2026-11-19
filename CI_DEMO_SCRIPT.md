# Classroom Demonstration: A Failing Check Blocks a Merge

**Goal:** observe the relationship between a regression, a test failure and a required status check.

Allow about 10–15 minutes, including CI queue and execution time. Use a personal fork for the exercise. The [walkthrough](CI_DISCIPLINE_WALKTHROUGH.md) explains the mechanism; this page is the practical run sheet.

## 1. Prepare the learning repository

Fork HexaStock and clone your fork. Use JDK 21 and start Docker if you intend to run the complete suite locally. Enable GitHub Actions in the fork if necessary.

Forks copy source, not upstream branch-protection settings. To demonstrate **blocked merging**, configure a ruleset for the fork's `main` that requires a pull request and the `build-and-test` check. Without it, a red check is visible but may not prevent a merge.

Start from a clean checkout of your fork's `main`:

```bash
git status --short
git pull --ff-only
git switch -c demo/ci-regression
./mvnw -pl domain test
```

Expected baseline: the domain suite passes. Record the current count rather than assuming a fixed historical total.

## 2. Introduce one controlled regression

In `domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Portfolio.java`, temporarily comment out this line inside `buy`:

```java
balance = balance.subtract(totalCost);
```

Run:

```bash
./mvnw -pl domain test
```

Tests checking the purchase balance should fail. Explain the failure before looking at CI: a purchase creates shares without paying for them.

Stage only the intended file and push the exercise branch:

```bash
git add domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Portfolio.java
git commit -m "Demo: omit purchase cash deduction"
git push -u origin demo/ci-regression
```

Open a pull request **in your fork**, targeting its `main`. Confirm the base repository in the UI; do not send the intentional regression to the upstream project.

## 3. Observe the failed check

Open the `build-and-test` details. Compare the CI failure with the local result. If the fork's ruleset requires the check, merging is blocked.

A failure early in the reactor can finish faster than a successful full build because later modules do not run. Queue time and dependency downloads still vary.

## 4. Restore the behavior

Uncomment the cash-deduction line, then run the domain tests again. Inspect the diff to ensure only the intended restoration is present.

```bash
./mvnw -pl domain test
git add domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Portfolio.java
git commit -m "Demo: restore purchase cash deduction"
git push
```

Wait for checks on the new commit. The successful full build also runs the database-backed tests. Other rules may still affect merge availability.

Close the demonstration pull request without merging. The exercise's purpose is to observe protection, not to add deliberate regressions to the project history.

## 5. Discuss what was demonstrated

- Which assertion detected the missing cash update?
- Which workflow command ran the tests?
- Which repository rule prevented the merge?
- Could the check pass if someone removed the assertion instead of fixing the code?

The final question leads to code review and test quality. A green status proves that the executed checks passed, not that every requirement has been tested.

If a direct push was rejected or reports are missing, consult [the walkthrough](CI_DISCIPLINE_WALKTHROUGH.md) and [CI configuration](CI_SETUP.md).
