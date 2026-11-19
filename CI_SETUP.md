# CI Configuration and Report Locations

**Goal:** understand which checks the repository runs and where their evidence is produced.

## The checked-in workflow

[build.yml](https://github.com/alfredorueda/HexaStock/blob/main/.github/workflows/build.yml) defines one job, `build-and-test`. It runs for pushes to `main` and pull requests targeting `main`.

The job checks out the repository, installs Temurin JDK 21, prints Docker diagnostics and runs:

```bash
./mvnw -B -U -ntp clean verify
```

Testcontainers supplies MySQL and MongoDB for database-backed tests. The workflow has a 20-minute timeout. A successful build is a test result, not a deployment: this file defines CI, not an application release pipeline.

## Find reports in a multi-module build

| Evidence | Generated location |
|---|---|
| Surefire results | Each module's `target/surefire-reports/` |
| Failsafe results, when applicable | Each module's `target/failsafe-reports/` |
| Aggregate coverage | `bootstrap/target/site/jacoco-aggregate/` |

The current workflow's artifact-upload and failure-diagnostic paths still point to root-level `target` directories, including `target/site/jacoco-merged`. These do not match the module report locations. An upload step completing successfully therefore does not establish that the intended reports were found. Inspect its logs and available artifacts.

The same obsolete merged-coverage path appears in `sonar-project.properties`. Any scanner-based coverage integration must point to the report actually produced by the Maven build.

## Distinguish Sonar analysis from this workflow

SonarCloud checks and badges may be visible on pull requests through the connected service. The checked-in `build.yml` does not contain a Sonar scanner invocation or a quality-gate step. Do not infer from a badge that this workflow uploaded JaCoCo coverage or that the Sonar check is a required merge condition.

`sonar-project.properties` references `SONAR_ORGANIZATION`, `SONAR_PROJECT_KEY` and `SONAR_HOST_URL`. Those placeholders describe configuration inputs; declaring them does not run an analysis. For a future scanner integration, configure the project, supply credentials through secrets, select the correct compiled-class and coverage paths, and verify the analysis logs. Keep tokens out of source control.

## Inspect branch protection

The repository uses a ruleset for `main`, requiring a pull request and the `build-and-test` status check. Protection is hosted GitHub configuration, separate from the workflow YAML. Inspect the active rules when reproducing the setup in another repository:

```bash
gh api repos/alfredorueda/HexaStock/rules/branches/main
```

Forking the source does not reproduce the upstream repository's protection settings. A visible check is only a merge gate if the active rules require it.

## Verify a setup change

1. Run the documented Maven command locally with JDK 21 and Docker.
2. Open a focused pull request and inspect the check for its latest commit.
3. Read failed-test output or module reports when the build fails.
4. Confirm artifact contents rather than relying on the upload step's status.
5. Confirm which checks the repository requires before merging.

Continue with the [CI explanation](CI_DISCIPLINE_WALKTHROUGH.md) or the [classroom demonstration](CI_DEMO_SCRIPT.md).
