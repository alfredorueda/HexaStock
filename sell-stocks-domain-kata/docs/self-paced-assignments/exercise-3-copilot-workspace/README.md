# Exercise 3 — Student guide

You will rebuild the Exercise 2 domain with GitHub Copilot, one skill, and three specialised
agents. Your starter workspace is already prepared. Do not run a setup or generation script.

## Before you start

1. Open this entire folder in VS Code. Do not open only an individual file.
2. Confirm that Java 21, Maven, Git, and your GitHub Copilot licence are available.
3. Open Copilot Chat.
4. Run **Chat: Open Customizations** and confirm that you can see:
   - `Specification Planner`;
   - `Domain Implementer`;
   - `Conformance Reviewer`;
   - `/create-plan`, `/implement-approved-plan`, and `/verify-conformance`; and
   - the `implement-sell-stocks` skill.

If any item is missing, stop and ask the instructor. Do not edit configuration files during the
exercise.

## Step 1 — Plan

Run this in Copilot Chat:

```text
/create-plan
```

The planner reads the specifications but cannot edit your files. When it returns a proposed plan:

1. copy the proposal into `plan.md`;
2. review it and improve at least one item yourself;
3. check that AC-01 through AC-24 are all covered;
4. change the plan status to `approved`;
5. set the Human checkpoint decision to `approved` and add your name and date; and
6. commit the approved plan.

Do not continue until you understand and approve the plan.

## Step 2 — Generate the Java project

Run:

```text
/implement-approved-plan
```

This is the command that generates `pom.xml`, `src/main`, and `src/test`. Review each checkpoint
and proposed diff. Do not approve all edits automatically.

## Step 3 — Independent review

Run:

```text
/verify-conformance
```

The reviewer must report findings without editing the solution. Decide which findings should be
returned to the implementer.

## Step 4 — Final check

Run this yourself from the workspace root:

```bash
mvn test
```

The expected result is exactly **36 tests**, with no failures or errors.

If Bash is available, the reviewer or instructor may also run:

```bash
.agents/skills/implement-sell-stocks/scripts/verify-workspace.sh
```

This helper is not required when the managed workstation does not provide Bash. In that case, the
reviewer follows the bundled review checklist and records the environment limitation in
`evidence/session-notes.md`. A successful `mvn test` remains mandatory.

## Submit

- the generated code and tests;
- the approved and completed `plan.md`;
- the completed `evidence/session-notes.md`;
- the final Maven test output; and
- your Git history or final diff.

Remember the workflow:

```text
/create-plan → approve plan.md → /implement-approved-plan → /verify-conformance → mvn test
```
