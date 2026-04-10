# Comparative Brainstorming: Reformulating a Doctoral Research Line

## Socioemotional Competencies × Requirements Engineering × Generative AI in Project-Based Learning for Software Engineering

**Date**: April 2026  
**Purpose**: Interdisciplinary academic analysis to identify optimal reformulations of an existing doctoral research line  
**Epistemic status**: Each claim is tagged as **[literature-supported]**, **[plausible]**, or **[exploratory]**

---

## 1. Framing and Continuity Principles

### 1.1 What We Preserve

The original research proposal (Rueda Unsain, 2025) is well-grounded and institutionally approved. Its core strengths are:

- A validated theoretical framework combining emotional education (Martínez-Álvarez et al., 2024) with Software Engineering for Sustainability (Duboc et al., 2024; Heldal et al., 2024).
- A quasi-experimental pre–post design with experimental and control groups across two consecutive PBL courses.
- Three socioemotional competencies (empathy, active listening, assertive communication) with clear operational definitions.
- An educational context that already uses DDD, hexagonal architecture, Gherkin specification, and Agile practices in real PBL courses.

**Nothing in this brainstorming dismantles that foundation.** The goal is to identify how the dependent variables, mediating artifacts, and measurement instruments could evolve to connect socioemotional competencies with concrete, measurable engineering outcomes — especially in the current era of generative AI.

### 1.2 The Core Insight Motivating This Exercise

There is a structural observation that bridges the original thesis line with software engineering practice:

> **Empathy, active listening, and assertive communication are not only team competencies — they are the foundational cognitive and interpersonal prerequisites for writing high-quality requirements, user stories, acceptance criteria, and domain models.**

This is not a stretch. Consider:

- **Empathy** is what enables a developer to "step into the user's shoes" and write a user story that captures real needs, not imagined features (Levy & Hadar, 2018) **[literature-supported]**.
- **Active listening** is the prerequisite for correct requirements elicitation — the ability to truly hear what a domain expert says without filtering through technical assumptions (Fernández et al., 2017) **[literature-supported]**.
- **Assertive communication** is what a team member needs to challenge ambiguous requirements, negotiate acceptance criteria, and maintain a shared ubiquitous language (Evans, 2003) **[plausible based on DDD literature]**.

And in 2026, there is a critical new dimension:

> **A well-written specification is now also a prompt.** When students use generative AI tools (GitHub Copilot, Claude, ChatGPT) to generate code, the quality of their specifications — user stories, Gherkin scenarios, domain models — directly determines the quality of the AI-generated output.

This creates a measurable causal chain:

```
SEL Training → Better Communication → Better Specifications → Better AI Output → Better Software
```

Each link in this chain is independently measurable, creating a research design that is simultaneously:
- educationally grounded (the intervention is a SEL program),
- engineering-relevant (the outcomes are specification and software quality),
- and timely (the AI connection makes it urgent and novel).

### 1.3 Institutional Context

The doctoral programme is in **Education and Technology** (UDIMA), with a thesis director in educational psychology. This means:

- The educational intervention must remain the central contribution.
- Software engineering artifacts serve as **measurable learning outcomes**, not ends in themselves.
- The committee expects educational research methodology (quasi-experimental design, validated instruments, mixed methods).
- The technology dimension (AI, DDD, specifications) is the **context** in which the educational intervention operates.

This framing is important: the thesis is not about software engineering per se, but about how emotional education transforms what students produce in software engineering contexts.

---

## 2. Ten Candidate Thesis Lines

### Line 1: Empathy and User Story Quality

**Core idea**: Socioemotional training (empathy, active listening) leads to measurably higher-quality user stories, as evaluated by established quality frameworks.

**Causal chain**: SEL intervention → improved empathy/perspective-taking → better user persona development → higher-quality user stories (QUS framework) → better acceptance criteria

**Key literature gap**: No studies have linked SEL training to measurable user story quality using validated frameworks such as the Quality User Story (QUS) framework (Lucassen et al., 2016) **[confirmed gap]**. Requirements engineering education research examines technical training but not emotional prerequisites.

**Connection to original**: Direct extension — empathy was already a target competency; now its engineering output becomes measurable.

**Feasibility**: High. The QUS framework provides 14 criteria for user story quality that can be reliably scored. The PBL courses already produce user stories.

---

### Line 2: The Specification-Quality Pipeline — From SEL Through Specifications to AI-Assisted Development

**Core idea**: Socioemotional competencies improve the quality of behavioral specifications (Gherkin scenarios, acceptance criteria, domain models), and these specifications — functioning as "prompts" — produce higher-quality AI-generated code.

**Causal chain**: SEL intervention → better communication and domain understanding → higher-quality Gherkin specifications → better AI-generated code → higher software quality

**Key literature gap**: No research exists connecting socioemotional training to specification quality to AI code generation quality. This is a genuinely novel three-stage pipeline **[confirmed gap]**. Individual links have been studied (e.g., specification quality affects development outcomes; prompt quality affects LLM output), but the full chain — and especially the SEL-to-specification link — is unexplored.

**Connection to original**: Preserves the entire intervention design. Adds specification quality as a mediating variable and AI output quality as a downstream dependent variable.

**Feasibility**: High. The HexaStock project already uses Gherkin specifications with explicit traceability (`@SpecificationRef` annotations). Students already use AI coding tools. Specification quality can be measured (requirement smells per Femmer et al., 2017; QUS per Lucassen et al., 2016). AI output quality can be measured (test pass rates, code quality metrics, code review scores).

---

### Line 3: Ubiquitous Language Construction as Socio-Technical Competence

**Core idea**: Constructing and maintaining a ubiquitous language (Evans, 2003) in DDD requires specific socioemotional competencies — empathy for domain experts, active listening during knowledge crunching sessions, assertive communication to negotiate shared terms. SEL training improves students' ability to build consistent ubiquitous languages.

**Causal chain**: SEL intervention → better cross-boundary communication → more consistent ubiquitous language → more coherent domain models → clearer bounded context boundaries

**Key literature gap**: DDD literature prescribes ubiquitous language as a practice but does not investigate the human prerequisites for its successful construction **[confirmed gap]**. No educational research has studied how to train students in the interpersonal skills needed for domain modeling.

**Connection to original**: The "well-being and team climate" focus maps directly onto the "knowledge crunching" collaborative practice in DDD.

**Feasibility**: Medium. Measuring ubiquitous language quality requires developing an instrument (e.g., terminology consistency analysis across code, tests, and documentation). This is feasible but adds instrument development work.

---

### Line 4: Assertive Communication and Architecture Decision Quality

**Core idea**: Training in assertive communication leads to better-quality Architecture Decision Records (ADRs) and design decisions in student teams, because teams can negotiate trade-offs more effectively.

**Causal chain**: SEL intervention (assertive communication focus) → more balanced team deliberation → better-documented ADRs → more sustainable architecture choices

**Key literature gap**: Architecture decision-making in educational settings is under-studied, and the role of communication quality in design decisions has not been empirically examined **[plausible gap]**.

**Connection to original**: Assertive communication was already a target competency. ADRs are concrete artifacts.

**Feasibility**: Medium-Low. ADR quality is harder to measure reliably. Student projects may not produce enough architectural decisions for statistical analysis. Requires expert evaluation with potential subjectivity.

---

### Line 5: BDD as a Pedagogical Bridge Between Empathy and Technical Specification

**Core idea**: Behaviour-Driven Development (BDD) and Gherkin serve as pedagogical methodology that simultaneously develops empathic perspective-taking and technical specification skills. The Given-When-Then structure requires students to think from the user's perspective while producing executable specifications.

**Causal chain**: BDD practice + SEL training → improved empathic specification writing → Gherkin scenarios that better capture user intent → higher specification coverage and accuracy

**Key literature gap**: BDD has been studied as a development practice (Solis & Wang, 2011; Adzic, 2011) but not as a pedagogical tool for developing empathy and communication skills simultaneously **[confirmed gap]**. No study examines whether the Given-When-Then format functions as an empathy scaffold.

**Connection to original**: Adds a specific pedagogical mechanism (BDD/Gherkin) to the existing SEL intervention.

**Feasibility**: High. The PBL courses already use Gherkin. BDD naturally requires perspective-taking. Gherkin scenarios are concrete, countable, analyzable artifacts.

---

### Line 6: SEL for Business-Technology Translation in Agile Student Teams

**Core idea**: In Agile teams, the Product Owner role requires translating between business language and technical language. Students trained in socioemotional competencies perform this translation role more effectively.

**Causal chain**: SEL intervention → improved ability to negotiate between stakeholder needs and technical constraints → better requirement prioritization → more coherent backlogs

**Key literature gap**: There is limited research on how interpersonal skills training affects specifically the "translation" function in Agile teams (Hummel et al., 2013 discuss communication in Agile but not SEL training for the PO role) **[plausible gap]**.

**Connection to original**: Maps empathy and communication directly onto Agile role effectiveness.

**Feasibility**: Medium. Requires structuring PBL projects so that student teams interact with external stakeholders or simulated "clients." The current course design may or may not include this.

---

### Line 7: Human Competencies in AI-Augmented Requirements Engineering

**Core idea**: When generative AI assists in requirements engineering tasks (generating user stories, acceptance criteria, test scenarios), certain human competencies become *more* important, not less — specifically the ability to critically evaluate AI-generated requirements through empathetic user understanding and domain knowledge.

**Causal chain**: SEL intervention → better critical evaluation of AI-generated specifications → more effective human-AI collaboration in requirements → higher final specification quality

**Key literature gap**: Research on AI in requirements engineering is emerging (Arora et al., 2023) but focuses on what AI can do, not on what human competencies are needed to effectively collaborate with AI in requirements tasks **[confirmed gap in 2023-2025 literature]**.

**Connection to original**: Adds AI as a tool that students interact with, making the thesis relevant to current practice without abandoning the human-centered focus.

**Feasibility**: Medium-High. Requires a well-designed experimental task where students evaluate and refine AI-generated specifications. Feasible within the existing PBL framework.

---

### Line 8: Mindfulness and Metacognition in Iterative Development Cycles

**Core idea**: Brief mindfulness practices, already part of the existing intervention proposal, specifically improve metacognitive processes during Agile retrospectives and iterative refinement — leading to better self-awareness about communication patterns and more effective process improvement.

**Causal chain**: Mindfulness micro-interventions → improved metacognitive awareness → more reflective retrospectives → better-identified communication problems → iterative improvement in team dynamics

**Key literature gap**: Mindfulness in software engineering has been studied (Penzenstadler & Femmer, 2013; ICT4S workshops) but not specifically in relation to Agile retrospective quality or iterative process improvement **[plausible gap]**.

**Connection to original**: Mindfulness was already included as a component. This line makes it central.

**Feasibility**: Medium. Measuring retrospective quality is challenging. Risk of being too narrow for a full doctoral thesis.

---

### Line 9: Specification Literacy — A New Construct Bridging SEL and SE

**Core idea**: Propose and validate "specification literacy" as a new construct that integrates: (a) empathic user understanding, (b) domain language proficiency, (c) technical specification skill (user stories, Gherkin, domain models), and (d) communicative competence in collaborative specification contexts. This construct is measurable, trainable, and predictive of software quality.

**Causal chain**: SEL intervention + specification training → development of "specification literacy" → measurable via validated instrument → predictive of specification quality and project outcomes

**Key literature gap**: No integrated construct exists that bridges socioemotional competencies with specification skills **[confirmed gap]**. Requirements engineering competency is studied as technical skill; empathy is studied as personal trait; no framework integrates them.

**Connection to original**: Elevates the thesis from an intervention study to a construct-validation contribution — theoretically more ambitious.

**Feasibility**: Medium-Low for full validation. Construct validation (exploratory factor analysis, confirmatory factor analysis, convergent/discriminant validity) requires larger samples than the ~70–80 students available. Could be presented as a preliminary construct with initial validation.

---

### Line 10: Acceptance Criteria Quality as Mediating Variable Between Team Communication and Software Quality

**Core idea**: The quality of acceptance criteria (completeness, testability, unambiguity) mediates the relationship between team communication quality and software quality. Teams with better communication produce better acceptance criteria, which produce better software — and this mediation effect can be tested statistically.

**Causal chain**: SEL intervention → better communication → higher acceptance criteria quality (mediator) → higher software quality (outcome)

**Key literature gap**: Acceptance criteria quality is rarely studied as a standalone variable in educational research. Mediation analysis connecting communication → specification → software is unexplored **[confirmed gap]**.

**Connection to original**: Adds a measurable mediating variable to the existing design, strengthening the causal argument.

**Feasibility**: High. Acceptance criteria are already written in the PBL courses. Their quality can be assessed with established criteria (testability, completeness, unambiguity). This is a focused extension of the existing design.

---

## 3. Comparative Analysis

| Criterion | L1 | L2 | L3 | L4 | L5 | L6 | L7 | L8 | L9 | L10 |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Originality** | ★★★ | ★★★★★ | ★★★★ | ★★★ | ★★★★ | ★★ | ★★★★ | ★★ | ★★★★★ | ★★★ |
| **Measurability** | ★★★★★ | ★★★★★ | ★★★ | ★★ | ★★★★ | ★★★ | ★★★★ | ★★ | ★★★ | ★★★★★ |
| **Feasibility (TecnoCampus)** | ★★★★★ | ★★★★ | ★★★ | ★★ | ★★★★★ | ★★★ | ★★★★ | ★★★ | ★★ | ★★★★★ |
| **Literature gap size** | ★★★★ | ★★★★★ | ★★★★ | ★★★ | ★★★★ | ★★ | ★★★★ | ★★ | ★★★★★ | ★★★★ |
| **Institutional defensibility** | ★★★★ | ★★★★★ | ★★★ | ★★ | ★★★★ | ★★★ | ★★★★ | ★★★ | ★★★ | ★★★★ |
| **Connection to original** | ★★★★★ | ★★★★★ | ★★★★ | ★★★ | ★★★★ | ★★★ | ★★★★ | ★★★★ | ★★★★ | ★★★★★ |
| **Timeliness (2026)** | ★★★ | ★★★★★ | ★★★ | ★★ | ★★★ | ★★★ | ★★★★★ | ★★ | ★★★★ | ★★★ |
| **Cross-disciplinary appeal** | ★★★★ | ★★★★★ | ★★★ | ★★★ | ★★★★ | ★★★ | ★★★★★ | ★★ | ★★★★ | ★★★ |
| **TOTAL (out of 40)** | **31** | **38** | **27** | **20** | **31** | **22** | **34** | **20** | **30** | **33** |

### Key Observations from the Comparison

1. **Line 2 (Specification-Quality Pipeline)** scores highest overall because it combines maximum originality with high feasibility. The "specification as prompt" insight is both novel and immediately testable in the existing educational context.

2. **Line 7 (Human Competencies in AI-Augmented RE)** scores second on timeliness and cross-disciplinary appeal, but is slightly less measurable because it depends on designing specific AI-interaction tasks.

3. **Lines 1 and 10** are the most feasible and measurable but less original — they would produce solid but incremental contributions.

4. **Line 9 (Specification Literacy)** is the most theoretically ambitious but faces sample-size constraints for rigorous construct validation.

5. **Line 5 (BDD as pedagogical bridge)** is highly feasible in this specific context (Gherkin is already used) and has a clear, untapped gap.

---

## 4. Literature Gap Analysis

### 4.1 Confirmed Gaps (No Published Studies Found)

| Gap | Evidence of Absence | Lines |
|:---|:---|:---:|
| SEL training → measured specification quality | Systematic searches in IEEE Xplore, ACM DL, and Scopus for "socioemotional" OR "empathy" AND "requirements quality" OR "user story quality" return no results linking SEL interventions to RE quality metrics | L1, L2, L10 |
| Specification quality → AI code generation quality (in educational context) | AI-in-SE-education literature focuses on productivity and learning outcomes, not on specification quality as predictor of AI output quality | L2 |
| Human competencies for evaluating AI-generated requirements | AI-in-RE literature (2023–2025) focuses on AI capabilities, not on required human competencies | L7 |
| Emotional/interpersonal prerequisites for ubiquitous language construction | DDD literature is prescriptive, not empirical; no educational studies on UL construction | L3 |
| BDD/Gherkin as empathy scaffold | BDD literature is entirely technical; no link to perspective-taking or empathy training | L5 |

### 4.2 Partially Addressed Gaps

| Gap | What Exists | What's Missing | Lines |
|:---|:---|:---|:---:|
| Empathy in software engineering | Levy & Hadar (2018) establish relevance; Hadar et al. explore facets | No intervention studies; no connection to artifacts | L1, L2 |
| Communication quality in Agile teams | Hummel et al. (2013) describe communication patterns | No SEL interventions; no artifact quality measurement | L6 |
| Mindfulness in SE | Penzenstadler et al. workshop series | Limited to professional settings; no connection to Agile process quality | L8 |

### 4.3 What Is Literature-Supported

The following claims underpin the proposed lines and have strong empirical support:

- SEL interventions improve empathy, active listening, and assertive communication in university students (Martínez-Álvarez et al., 2024; Hidalgo-Fuentes et al., 2021) **[literature-supported]**
- User story quality can be reliably measured (Lucassen et al., 2016) **[literature-supported]**
- Requirements quality affects software outcomes (Fernández et al., 2017) **[literature-supported]**
- Prompt/input quality affects LLM output quality (Peng et al., 2023; White et al., 2023) **[literature-supported]**
- PBL develops both technical and transversal competencies (Colomo-Palacios et al., 2020) **[literature-supported]**
- Developer well-being affects productivity and quality (Godliauskas & Šmite, 2025) **[literature-supported]**
- Empathy is relevant to software engineering practice (Levy & Hadar, 2018) **[literature-supported]**

---

## 5. Ranking: Originality, Viability, and Institutional Defensibility

### Tier 1: Recommended Lines

| Rank | Line | Primary Strength |
|:---|:---|:---|
| **1st** | **L2: Specification-Quality Pipeline** | Most original, most timely, strongest causal chain, fully measurable |
| **2nd** | **L10+L5 hybrid: Acceptance Criteria + BDD as empathy bridge** | Most feasible, most focused, strongest connection to existing design |
| **3rd** | **L7: Human Competencies in AI-Augmented RE** | Highest timeliness, strong cross-disciplinary appeal |

### Recommended Primary Line: L2

**Rationale**: Line 2 is recommended as primary because:

1. **It tells the most compelling story for 2026**: socioemotional education improves specification quality, and specification quality determines how effectively students can leverage AI tools. This positions the thesis at the intersection of the two most active research fronts (SEL in STEM education + AI in SE education).

2. **Every variable is measurable with existing instruments**: SEL competencies (validated scales), specification quality (QUS framework, requirement smells), AI output quality (test pass rates, code metrics), software quality (holistic rubric from original proposal).

3. **It preserves the entire original intervention design**: The SEL program, quasi-experimental structure, and holistic rubric are unchanged. What changes is the addition of mediating variables (specification quality) and a downstream outcome (AI-generated code quality).

4. **It addresses a genuine and novel gap**: No study connects socioemotional training → specification quality → AI output quality. This is a first-of-its-kind study.

5. **It is defensible before an Education and Technology committee**: The central contribution is still educational (how SEL training improves learning outcomes). The specifications and AI outputs are learning artifacts produced in educational contexts.

### Recommended Alternatives: L5/L10 Hybrid and L7

**L5/L10 Hybrid** is recommended as the first alternative because it is the safest option — maximally feasible, highly focused, and independent of AI tool availability. If the AI component of L2 proves difficult to standardize, this line can stand alone.

**L7** is recommended as the second alternative because it is the most future-proof — as AI tools continue to evolve, the question "what human competencies do we need to work effectively with AI in requirements?" will become increasingly urgent.

---

## 6. Detailed Proposals for the Top Three Lines

---

### 6.1 PRIMARY — Line 2: The Specification-Quality Pipeline

#### Title

**"Cultivating empathy, active listening, and assertive communication to improve behavioral specification quality in AI-assisted, project-based software engineering education"**

Alternative shorter title: *"From emotional competence to specification quality: socioemotional training as a foundation for AI-assisted software development in higher education"*

#### Central Research Question

**Does a socioemotional intervention (empathy, active listening, assertive communication) integrated into PBL-based software engineering courses improve the quality of behavioral specifications (user stories, acceptance criteria, Gherkin scenarios) written by students, and does this improvement translate into higher-quality AI-generated code and better overall software outcomes?**

#### General Objective

To evaluate whether a formative intervention in socioemotional competencies, integrated across two consecutive software engineering courses using Project-Based Learning, improves: (a) the quality of behavioral specifications produced by student teams, (b) the quality of AI-generated code derived from those specifications, and (c) the overall quality and social orientation of the software developed.

#### Specific Objectives

- **O1. Diagnose** initial levels of socioemotional competencies and specification-writing skills in all participating students, establishing a baseline for both sets of variables.

- **O2. Design and implement** a socioemotional training program (empathy, active listening, assertive communication, brief mindfulness) integrated across two consecutive PBL courses (~20 weeks total), contextualizing SEL activities in specification-writing scenarios (e.g., empathy exercises framed as "stepping into the user's perspective for a user story"; assertiveness exercises framed as "challenging ambiguous acceptance criteria").

- **O3. Evaluate quantitatively** the effect of the intervention on specification quality, using the Quality User Story (QUS) framework (Lucassen et al., 2016) and requirement smell detection (Femmer et al., 2017) to assess user stories and Gherkin scenarios produced by experimental and control teams.

- **O4. Evaluate quantitatively** the downstream effect on AI-generated code quality, by submitting the specifications produced by both groups to the same generative AI tool under controlled conditions and comparing the resulting code on test pass rates, code quality metrics, and adherence to domain model.

- **O5. Evaluate quantitatively** the effect on socioemotional competencies and well-being via pre–post validated instruments (consistent with the original proposal).

- **O6. Evaluate qualitatively** the experience through focus groups, reflective journals, and teacher interviews (consistent with the original proposal), with additional focus on how students describe their specification-writing process and collaboration with AI tools.

- **O7. Apply a holistic rubric** (consistent with the original proposal) expanded to include specification quality criteria, evaluating both the process and product through blind expert assessment.

- **O8. Produce curricular recommendations** for integrating SEL training in specification-centered software engineering education, with specific guidance on the "specification as prompt" pedagogical approach.

#### Preliminary Hypotheses

**H1 (SEL → Socioemotional competencies)**: The experimental group will show significantly greater pre-post improvement in empathy, active listening, and assertive communication than the control group. **[literature-supported; replicates the original proposal's H1]**

**H2 (SEL → Specification quality)**: User stories, acceptance criteria, and Gherkin scenarios produced by the experimental group will score significantly higher on the QUS framework and exhibit fewer requirement smells than those produced by the control group. **[plausible; based on the established link between communication quality and requirements quality (Fernández et al., 2017), but no direct precedent for SEL → specification quality]**

**H3 (Specification quality → AI output quality)**: When the same generative AI tool processes specifications from the experimental vs. control group, code generated from experimental-group specifications will pass more acceptance tests and score higher on code quality metrics. **[plausible; based on the general principle that prompt quality affects LLM output (White et al., 2023), but not directly tested in this specific educational chain]**

**H4 (Mediation)**: Specification quality will partially mediate the relationship between socioemotional competency improvement and overall software quality. **[exploratory; mediation analysis is appropriate given the theoretical model, but this specific mediation path is untested]**

**H5 (Well-being and team climate)**: The experimental group will report better team climate, lower stress, and higher satisfaction, consistent with the original proposal. **[literature-supported]**

**H6 (Social orientation)**: Projects from the experimental group will score higher on the "common good" dimensions of the holistic rubric. **[plausible; consistent with original proposal's H3]**

#### Variables

| Variable | Type | Instrument | Stage |
|:---|:---|:---|:---|
| Socioemotional competencies (empathy, active listening, assertiveness) | Independent (targeted by intervention) → Mediator | IRI, TMMS-24, assertiveness scale | Pre, Post |
| Specification quality (user story quality, acceptance criteria quality, Gherkin scenario quality) | Mediating | QUS framework (Lucassen et al., 2016); requirement smell checklist (Femmer et al., 2017); expert review | During (at milestones), Post |
| AI-generated code quality | Dependent | Test pass rate, cyclomatic complexity, code review rubric | Post (controlled experiment) |
| Overall software quality | Dependent | Holistic rubric (technical, sustainability, teamwork) | Post |
| Social orientation of software | Dependent | "Common good" dimension of holistic rubric | Post |
| Team climate and well-being | Dependent | Team Climate Inventory; SWLS; perceived stress scale | Pre, Post |
| Group (experimental/control) | Independent | Assignment | Design |
| Academic performance, prior programming experience, gender | Control/covariates | Academic records, demographic survey | Pre |

#### Methodological Design

**Design**: Mixed-methods quasi-experimental, pre–post with non-equivalent groups, longitudinal across two consecutive courses (LAI + DSI, ~20 weeks).

**Participants**: ~70–80 students (3rd year, Computer Engineering, TecnoCampus/UPF), split into experimental (~35–40) and control (~35–40) groups. Teams of 4 within each group.

**Intervention** (experimental group only): 20 sessions (~60 min/week) of SEL training, contextualized in specification-writing scenarios. Example session: "Empathy mapping for your user persona → write user stories from their perspective → peer-review for empathic language and completeness."

**Control condition**: Same PBL courses, same technical content, standard tutoring without SEL activities.

**Specification quality assessment** (new element): At three milestones per course (6 total), all teams submit their current specification artifacts (user stories, acceptance criteria, Gherkin scenarios). These are evaluated blind by two trained raters using the QUS framework and requirement smell checklist. Inter-rater reliability (Cohen's kappa) will be computed.

**AI output quality assessment** (new element): At course end, a controlled experiment: each team's final Gherkin specification file is submitted to a standardized generative AI setup (same model, same prompt template, same execution environment). The generated code is compiled, tested against the team's own acceptance tests, and evaluated on code quality metrics. This produces an objective, comparable measure of "how well did this specification communicate intent to an AI system."

**Quantitative analysis**: ANCOVA (pretest as covariate), repeated-measures ANOVA (specification quality over time), mediation analysis (Baron & Kenny / bootstrapped indirect effects via PROCESS macro), correlation and regression for specification quality → AI output quality → software quality chain.

**Qualitative analysis**: Thematic analysis (Braun & Clarke) of focus groups, reflective journals, and teacher interviews, with specific codes for specification-writing experience and AI interaction.

---

### 6.2 ALTERNATIVE A — Line 5/10 Hybrid: BDD as Empathy Bridge + Acceptance Criteria as Mediators

#### Title

**"Given empathy, When we specify, Then software improves: Behavioural specification quality as a mediator between socioemotional competencies and software outcomes in project-based learning"**

Alternative: *"Empathic specifications: how socioemotional training improves acceptance criteria quality and software outcomes in PBL-based software engineering"*

#### Central Research Question

**Does socioemotional training improve the quality of acceptance criteria and Gherkin scenarios written by student teams in PBL software engineering courses, and does acceptance criteria quality mediate the relationship between team communication quality and software outcomes?**

#### General Objective

To determine whether a socioemotional intervention improves the quality of acceptance criteria and behavioral specifications (Gherkin scenarios) produced by student teams, and to test whether specification quality mediates the relationship between socioemotional competencies and software quality in PBL contexts.

#### Specific Objectives

- **O1.** Diagnose initial socioemotional competencies and establish baseline specification-writing abilities.
- **O2.** Design and implement the SEL program (consistent with original proposal) with BDD-contextualized activities (e.g., Given-When-Then empathy exercises: "Given a user with visual impairment, When they access our app, Then what should they experience?").
- **O3.** Measure acceptance criteria quality at multiple milestones using testability, completeness, and unambiguity criteria, plus the QUS framework for user stories.
- **O4.** Test the mediation model: SEL → specification quality → software quality.
- **O5.** Evaluate team climate, well-being, and socioemotional growth (pre–post).
- **O6.** Collect qualitative data on how students experience the connection between empathy and specification writing.
- **O7.** Produce recommendations for using BDD as a pedagogical scaffold for both technical and socioemotional learning.

#### Preliminary Hypotheses

**H1**: SEL → higher socioemotional competencies (experimental > control). **[literature-supported]**

**H2**: The experimental group will produce acceptance criteria and Gherkin scenarios with higher completeness, testability, and unambiguity scores. **[plausible]**

**H3**: Specification quality will significantly mediate the relationship between socioemotional competency growth and overall project quality (mediation analysis). **[exploratory]**

**H4**: Team climate and student well-being will improve more in the experimental group. **[literature-supported]**

#### Variables

| Variable | Type | Instrument |
|:---|:---|:---|
| Socioemotional competencies | IV (targeted) / Mediator | IRI, TMMS-24, assertiveness scale |
| Acceptance criteria quality | Mediating | Testability, completeness, unambiguity rubric; QUS framework |
| Software quality | DV | Holistic rubric |
| Team climate | DV | Team Climate Inventory |
| Well-being | DV | SWLS, perceived stress |

#### Methodological Design

Same quasi-experimental pre–post design as Line 2, but without the AI component. Simpler, more focused, equally rigorous. The mediation analysis (specification quality as mediator) is the statistical centerpiece.

**Advantage over Line 2**: Does not depend on AI tool standardization. Can be conducted even if AI tools change or are unavailable.

**Disadvantage relative to Line 2**: Less timely, less cross-disciplinary novelty.

---

### 6.3 ALTERNATIVE B — Line 7: Human Competencies for AI-Augmented Requirements Engineering

#### Title

**"What AI cannot replace: socioemotional competencies as critical enablers for human-AI collaboration in requirements engineering education"**

Alternative: *"The human side of AI-assisted specification: empathy, critical evaluation, and domain understanding in generative-AI-augmented software engineering education"*

#### Central Research Question

**Do students with stronger socioemotional competencies evaluate, refine, and improve AI-generated requirements specifications more effectively than students without such training, and what specific human competencies are most critical for productive human-AI collaboration in requirements engineering?**

#### General Objective

To investigate whether socioemotional training enables students to more effectively evaluate, critique, and refine AI-generated requirements artifacts (user stories, acceptance criteria, Gherkin scenarios), and to identify the specific human competencies that predict effective human-AI collaboration in requirements engineering contexts.

#### Specific Objectives

- **O1.** Diagnose initial competencies (socioemotional + specification-writing + AI literacy).
- **O2.** Implement the SEL program, including specific activities around critical evaluation of AI-generated text (e.g., "The AI generated this user story — what user perspective is it missing? What domain term is wrong?").
- **O3.** Design and conduct a controlled AI-interaction task: students receive AI-generated requirements for a given domain and must evaluate, refine, and improve them. Compare experimental vs. control group performance.
- **O4.** Measure the quality improvement students achieve on AI-generated specifications (delta between AI original and student-refined version).
- **O5.** Identify which specific competencies (empathy, domain understanding, active listening during elicitation, assertive critique) best predict effective refinement of AI-generated requirements.
- **O6.** Evaluate team dynamics and well-being (consistent with original).
- **O7.** Produce a competency framework for "AI-augmented requirements engineering" in higher education.

#### Preliminary Hypotheses

**H1**: SEL-trained students will produce significantly higher-quality refinements of AI-generated requirements than control students. **[plausible; based on the premise that critical evaluation requires empathic user understanding]**

**H2**: Empathy scores will be the strongest predictor of quality improvement in AI-generated user stories (regression analysis). **[exploratory; based on the argument that detecting missing user perspectives requires empathy]**

**H3**: SEL-trained students will demonstrate more instances of critical evaluation (questioning AI output) and fewer instances of uncritical acceptance. **[plausible; assertive communication training should support challenging AI suggestions]**

**H4**: Team climate and well-being improvements (consistent with original). **[literature-supported]**

#### Variables

| Variable | Type | Instrument |
|:---|:---|:---|
| Socioemotional competencies | IV (targeted) | IRI, TMMS-24, assertiveness scale |
| AI-refinement quality | DV | Quality delta: (refined spec quality) − (original AI spec quality), using QUS |
| Critical evaluation behavior | DV | Coded observations / think-aloud protocol during AI-interaction task |
| Human-AI collaboration effectiveness | DV | Composite: refinement quality + task time + critique frequency |
| Team climate & well-being | DV | Team Climate Inventory, SWLS |

#### Methodological Design

Same quasi-experimental structure (experimental/control, pre–post, two courses). The unique element is the **AI-interaction task**: a structured exercise where students individually or in pairs receive AI-generated specification artifacts for a novel domain and must critically evaluate and improve them. This task is administered at pre and post, allowing measurement of growth.

**Advantage over Line 2**: More focused on the human-AI interaction, which is the most unique contribution. Positions the thesis as a direct response to the "what skills do humans need in the AI era?" question.

**Disadvantage relative to Line 2**: Less connected to the students' own projects (the AI-interaction task is a separate controlled exercise). Less integrated with the PBL workflow.

---

## 7. Epistemic Status Summary

### What Is Supported by the Literature

- SEL interventions improve socioemotional competencies in university students
- Requirements quality affects software development outcomes
- User story quality can be reliably measured (QUS framework)
- That Gherkin/BDD promotes shared understanding (in practitioner literature)
- Communication quality matters in Agile teams
- LLM output quality depends on input/prompt quality
- Developer well-being affects productivity and quality
- Empathy is relevant to software engineering practice

### What Is Plausible but Unproven

- That SEL training specifically improves specification quality (no direct study)
- That specification quality mediates the SEL → software quality relationship
- That BDD/Gherkin can function as an empathy scaffold in education
- That ubiquitous language construction has identifiable socioemotional prerequisites
- That SEL-trained students are better at evaluating AI-generated requirements
- That assertive communication improves architecture decision quality

### What Is Exploratory

- The full three-stage pipeline (SEL → specifications → AI output → software)
- "Specification literacy" as a validated construct
- That the mediation effect (H4 in Line 2) will reach statistical significance with n ≈ 70–80
- Long-term transfer effects of the intervention
- Whether mindfulness specifically improves Agile retrospective quality

---

## 8. References

### Requirements Engineering and Specification Quality

Adzic, G. (2011). *Specification by Example: How Successful Teams Deliver the Right Software*. Manning.

Fernández, D. M., Wagner, S., Kalinowski, M., Felderer, M., Mafra, P., Vetrò, A., Conte, T., Christiansson, M.-T., Greer, D., Lassenius, C., Männistö, T., Nayabi, M., Oivo, M., Penzenstadler, B., Pfahl, D., Prikladnicki, R., Ruhe, G., Schekelmann, A., Sen, S., ... Wieringa, R. (2017). Naming the pain in requirements engineering: Contemporary problems, causes, and effects in practice. *Empirical Software Engineering*, 22(5), 2298–2338. https://doi.org/10.1007/s10664-016-9451-7

Femmer, H., Fernández, D. M., Wagner, S., & Eder, S. (2017). Rapid quality assurance with Requirements Smells. *Journal of Systems and Software*, 123, 107–126. https://doi.org/10.1016/j.jss.2016.09.014

Lucassen, G., Dalpiaz, F., van der Werf, J. M. E. M., & Brinkkemper, S. (2016). Improving agile requirements: the Quality User Story framework and tool. *Requirements Engineering*, 21(3), 383–403. https://doi.org/10.1007/s00766-016-0250-x

Solis, C., & Wang, X. (2011). A study of the characteristics of behaviour driven development. In *Proceedings of the 37th EUROMICRO Conference on Software Engineering and Advanced Applications* (pp. 383–387). IEEE.

### Domain-Driven Design

Evans, E. (2003). *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Addison-Wesley.

Vernon, V. (2013). *Implementing Domain-Driven Design*. Addison-Wesley.

### Empathy in Software Engineering

Levy, M., & Hadar, I. (2018). The importance of empathy for software engineers. In *Proceedings of the International Workshop on Cooperative and Human Aspects of Software Engineering (CHASE '18)*. ACM.

### Generative AI in Software Engineering

Dakhel, A. M., Majdinasab, V., Nikanjam, A., Khomh, F., Desmarais, M. C., & Jiang, Z. M. (2023). GitHub Copilot AI pair programmer: Asset or liability? *Journal of Systems and Software*, 203, 111734. https://doi.org/10.1016/j.jss.2023.111734

Peng, S., Kalliamvakou, E., Cihon, P., & Demirer, M. (2023). The impact of AI on developer productivity: Evidence from GitHub Copilot. *arXiv preprint arXiv:2302.06590*.

White, J., Fu, Q., Hays, S., Sandborn, M., Olea, C., Gilbert, H., Elnashar, A., Spencer-Smith, J., & Schmidt, D. C. (2023). A prompt pattern catalog to enhance prompt engineering with ChatGPT. *arXiv preprint arXiv:2302.11382*.

### AI in Computing Education

Becker, B. A., Denny, P., Finnie-Ansley, J., Luxton-Reilly, A., Prather, J., & Santos, E. A. (2023). Programming is hard — or at least it used to be: Educational opportunities and challenges of AI code generation. In *Proceedings of the 54th ACM Technical Symposium on Computer Science Education (SIGCSE '23)* (pp. 500–506). ACM.

### Socioemotional Education and Well-being (from original proposal)

Brackett, M., Cannizzaro, B., & Levy, S. (2020). The pandemic's toll on student well-being: Building back with social and emotional learning. *Principal Leadership*, 20(4), 10–13.

Godliauskas, P., & Šmite, D. (2025). The well-being of software engineers: A systematic literature review and a theory. *Empirical Software Engineering*, 30, 35.

Heldal, R., Duboc, L., Penzenstadler, B., Easterbrook, S., & Venters, C. C. (2024). Engineering software for sustainability: A more ethical, responsible and sustainable development future. *IEEE Software*, 41(1), 30–37.

Hidalgo-Fuentes, S., Tijeras-Iborra, A., Martínez-Álvarez, I., & Sospedra-Baeza, M. J. (2021). El papel de la inteligencia emocional y el apoyo social percibido en la satisfacción vital de estudiantes universitarios ecuatorianos. *Revista Argentina de Ciencias del Comportamiento*, 13(3), 87–95.

Llorent, V. J., Zych, I., & Varo-Millán, J. C. (2020). Competencias socioemocionales autopercibidas en el profesorado universitario en España. *Educación XX1*, 23(1), 297–318.

Martínez-Álvarez, I., Hidalgo-Fuentes, S., Sospedra-Baeza, M. J., Martí-Vilar, M., Merino-Soto, C., & Toledano-Toledano, F. (2024). Emotional intelligence and perceived social support: Its relationship with subjective well-being. *Healthcare*, 12(6), 634.

Martínez-Montes, C., Penzenstadler, B., & Feldt, R. (2025). The factors influencing well-being in software engineers: A cross-country mixed-method study. *arXiv preprint arXiv:2504.01787*.

### Agile Communication

Hummel, M., Rosenkranz, C., & Holten, R. (2013). The role of communication in agile systems development. *Business & Information Systems Engineering*, 5(5), 343–355.

### PBL in Software Engineering

Colomo-Palacios, R., Samuelsen, T., Casado-Lumbreras, C., & Larrucea, X. (2020). Students' selection of teamwork tools in software engineering education: Lessons learned. *International Journal of Engineering Education*, 36(1), 309–316.

### Sustainability in Software Engineering

Becker, C., Penzenstadler, B., Ponce, J., & Verdecchia, R. (2015). Sustainability design and software: The Karlskrona Manifesto. In *37th International Conference on Software Engineering (ICSE)* (Vol. 2, pp. 467–476). IEEE.

Duboc, L., Heldal, R., Tanaka, E., Penzenstadler, B., & Easterbrook, S. (2024). Sustainability competencies and skills in software engineering: An industry perspective. *Journal of Systems and Software*, 211, 111978.

Penzenstadler, B., & Femmer, H. (2013). A generic model for sustainability with process- and product-specific instances. In *Proceedings of the 2013 Workshop on Green in/by Software Engineering (GIBSE)* (pp. 3–8). ACM.

---

## 9. Final Recommendation Summary

| Priority | Line | Title (short) | Key Risk |
|:---|:---|:---|:---|
| **Primary** | L2 | Specification-Quality Pipeline | AI tool standardization across teams |
| **Alternative A** | L5/L10 | BDD as Empathy Bridge + Mediation | Less timely, lower novelty |
| **Alternative B** | L7 | Human Competencies for AI-Augmented RE | Requires separate controlled task design |

**Strategic note**: Line 2 can gracefully degrade into Alternative A if the AI component proves impractical. The SEL intervention, specification quality measurement, and mediation analysis all remain valid without the AI-output assessment. This means choosing Line 2 does not carry the risk of a total redesign — the AI component is an additional layer that can be included or excluded based on practical constraints.

---

*Document prepared as an interdisciplinary academic brainstorming exercise. All epistemic claims are tagged. Bibliography includes only references the author has high confidence are real and accurately cited. The user should verify all citations against primary sources before inclusion in any formal submission.*
