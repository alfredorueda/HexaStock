# Exercises

Two ways of building the same thing. The point is the contrast between them.

## Exercise 1 — Build it from a prompt

*Not yet written — coming next.*

You describe the whole problem to an assistant in one conversation: the FIFO rule, the money
formulas, every case that should fail, every number. Whatever you forget to say, the assistant
decides for you. Then you try to change something, and discover that the only record of what you
asked for is a chat log.

## Exercise 2 — [Build it from specifications](exercise-2-specification-driven/README.md)

The same domain, but the behaviour lives in versioned files instead of in a conversation:

* [`spec/sell-stocks-spec.md`](exercise-2-specification-driven/spec/sell-stocks-spec.md) — the
  behaviour, as acceptance criteria AC-01 … AC-23.
* [`spec/domain-class-diagram.puml`](exercise-2-specification-driven/spec/domain-class-diagram.puml)
  — the structure.

The prompt shrinks to about thirty lines, and most of what remains is the technical stack. The
specifications carry everything else — which means they can be reviewed, diffed, corrected and
reused without anyone reading the conversation that produced the code.

Start at [exercise-2-specification-driven/README.md](exercise-2-specification-driven/README.md).
