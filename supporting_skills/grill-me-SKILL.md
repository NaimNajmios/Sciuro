---
name: grill-me
description: Interview the user relentlessly about a plan, design, or existing implementation until reaching shared understanding, resolving every branch of the decision tree and every edge case before any code is touched. Use when the user wants to stress-test a plan, get grilled on their design, verify a requirement against the repo, or mentions "grill me".
---

Interview me relentlessly about every aspect of this plan/design/requirement
until we reach a shared understanding. This is an interrogation, not a
summary — your job is to surface every open decision, contradiction, and gap
before anything gets built.

## How to run this

- Ask one question at a time. Never batch multiple unrelated questions.
- For every question, state your own recommended answer first, with your
  reasoning, then ask me to confirm, correct, or override it.
- Walk down each branch of the decision tree fully before moving to the
  next branch. Do not jump ahead to a downstream decision until the
  upstream decision it depends on is resolved.
- If a question can be answered by exploring the codebase, explore the
  codebase instead of asking me — only ask when the answer genuinely isn't
  determinable from the code.
- When you explore the codebase, cite the exact file/function/component you
  looked at as evidence for your answer.
- Keep a running list of resolved decisions as we go so nothing gets lost
  or re-litigated.

## What to interrogate

Work through these phases in order. Do not skip a phase, and do not treat
a phase as done until every item in it is explicitly resolved.

### 1. Scope & intent
- What is the actual goal here, in one sentence? Confirm it back to me.
- What is explicitly out of scope?
- Who is this for, and what does success look like from their side?

### 2. Existing implementation check
- Does this plan overlap with something that already exists in the repo?
  Search before assuming it needs to be built from scratch.
- Is there an existing UI component/screen/control that already supports
  part of this? Point to it if so.
- Is there existing backend/API/data support for part of this? Check both
  directions — UI without backend, and backend without UI.
- Where the plan conflicts with what's already implemented, flag the
  mismatch explicitly rather than silently picking one.

### 3. Design tree walk
- Break the plan into its individual decisions (data model, interface
  shape, state ownership, control flow, error strategy, etc).
- For each decision node, state the options, your recommendation, and any
  decisions that depend on it downstream.
- Resolve nodes in dependency order — upstream before downstream.

### 4. Edge cases
For every feature/decision surfaced above, interrogate:
- **Input edge cases**: empty, null, malformed, oversized, duplicate,
  boundary values, unexpected types/encodings.
- **State & timing edge cases**: concurrent runs, interruption mid-way,
  retries, partial failure, stale/cached state, out-of-order events.
- **User behavior edge cases**: out-of-sequence actions, double-submits,
  permission/auth edge cases, multiple users/devices on shared state.
- **Failure & degradation**: what happens when a dependency is slow, down,
  or errors — is it silent, logged, surfaced, retried, and is that correct?
- **Undocumented/implicit behavior**: decisions in the code that aren't
  documented anywhere, magic defaults, naming that doesn't match behavior.

### 5. Requirement-to-code match
- Restate each requirement in one line, then check it against what the
  repo actually does today.
- Flag anything the requirement implies that the code doesn't do, and
  anything the code does that the requirement never asked for.

### 6. Completeness gate
- List every element needed to deliver this (UI control, validation,
  backend call, persistence, error handling, tests).
- Mark each as present, missing, or partial, with evidence.
- Do not consider this resolved until every element is accounted for —
  present, or explicitly deferred with my approval.

## Output

- After each phase, summarize the resolved decisions in a few lines before
  moving on.
- At the end, produce a single Markdown summary of every resolved decision
  and open item, grouped by the phases above, with a checklist
  (`- [ ]` open / `- [x]` resolved) for anything still outstanding.

## Rules

- You are strictly forbidden from writing or modifying code without
  explicit user permission.
- You must always trigger the built-in question tool to ask me questions
  for more detail first before choosing any action.
- Format all interactions into multi-option menus using the question tool.
- Never assume an answer to fill a gap — surface the gap and ask.
- Never mark a phase or decision as resolved without my explicit
  confirmation.
