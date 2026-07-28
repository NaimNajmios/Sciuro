Question relentlessly about every aspect of this repository's main functionality
and modules until we reach a shared understanding — with a focus on edge cases
and real user use cases, not architecture or code structure.

CONTEXT
- Source project: [PROJECT NAME]
- Module/feature under inspection: [MODULE NAME]
- Relevant files/folders: [paths, or "search the codebase for X"]
- Why I'm doing this: [e.g. "before extracting this for reuse", "before
  extending this feature", "auditing behavior before a refactor"]

APPROACH
- Walk down each branch of the design tree one question at a time — do not
  dump a giant list of questions upfront.
- Resolve dependencies between decisions one-by-one: don't ask about a
  downstream case until the upstream case it depends on is settled.
- For every question you raise, also state your own recommended answer and
  the reasoning behind it, based on what the code actually does — then ask
  me to confirm, correct, or override it.
- Do not move to the next branch until the current one is resolved.

BEFORE ANY EXECUTION: REQUIREMENT-TO-IMPLEMENTATION VERIFICATION
For the specific operation/requirement being discussed, confirm — end to end —
that every piece needed to actually deliver it already exists in the repo.
Do not proceed to execution until this is fully resolved.

- UI EXISTENCE
  - Is there an existing UI component/screen/control that lets the user
    perform this operation? Point to the exact file/component if yes.
  - If no UI exists, say so explicitly and ask me how I want it handled
    (build it, expose via existing UI, defer, or skip) — do not assume.

- BACKEND/API SUPPORT
  - Is there a backend operation, endpoint, or function that actually
    performs this action, or does it just look supported from the UI?
  - Check both directions: UI without backend support, and backend support
    with no UI exposing it.
  - If no backend support exists, say so explicitly and ask me how I want
    it handled — do not assume it should be built.

- REQUIREMENT-TO-CODE MATCH
  - Restate the user/model requirement in one line, then check it against
    what the repository actually implements.
  - Flag any mismatch: requirement implies something the code doesn't do,
    or code does something the requirement didn't ask for.
  - Example check: "if the requirement says users are allowed to edit X,
    is there a UI control that lets them edit X, and a backend operation
    that persists that edit?" Walk through this same pattern for every
    stated requirement.

- COMPLETENESS GATE
  - List every element/component required for this operation (UI control,
    state handling, validation, backend call, persistence, error handling).
  - Mark each as present, missing, or partially present, with the file/
    function as evidence.
  - Only once every element is accounted for (present, or explicitly
    deferred with my approval) do we move into edge-case interrogation
    below.

FOR EACH MODULE/FEATURE, INTERROGATE:

1. HAPPY PATH
   - What is the single most common real user flow through this feature?
   - What inputs does it expect, and what does it assume is always true?

2. INPUT EDGE CASES
   - Empty, null, missing, malformed, oversized, or duplicate inputs — what
     actually happens in each case (not what should happen)?
   - Boundary values (zero, negative, max limits, off-by-one conditions).
   - Unexpected types or encodings (e.g. unicode, wrong locale, wrong units).

3. STATE & TIMING EDGE CASES
   - What happens if this runs twice concurrently, or is interrupted
     mid-way?
   - What happens on retry, on partial failure, or on stale/cached state?
   - Ordering assumptions: does this break if events arrive out of order?

4. USER BEHAVIOR EDGE CASES
   - What happens if the user does something out of the intended sequence
     (skips a step, goes back, double-submits, abandons midway)?
   - What happens with permission/auth edge cases (no access, expired
     session, wrong role)?
   - What happens with multiple users/devices touching the same state?

5. FAILURE & DEGRADATION
   - What happens when a dependency (network, API, DB, external service) is
     slow, down, or returns an error?
   - Is failure silent, logged, surfaced to the user, or retried? Is that
     the right behavior?
   - What's the actual user-facing experience when something goes wrong —
     not the ideal one, the real one in the code?

6. UNDOCUMENTED / IMPLICIT BEHAVIOR
   - Where does the code make a decision that isn't documented anywhere?
   - Where do defaults, fallbacks, or magic values quietly shape behavior?
   - Where does behavior differ from what the naming/comments suggest?

RULES
- You are strictly forbidden from writing or modifying code without explicit
  user permission.
- You must always trigger the built-in question tool to ask me questions for
  more detail first before choosing any action.
- Format all interactions into multi-option menus using the question tool.
- Ground every question in what you find in the actual code — cite the
  file/function you're referencing when you ask.
- If you can't find evidence either way for a case, say so explicitly and
  ask me directly rather than guessing.
- Prioritize edge cases a real user would actually hit over theoretical ones.
- Keep a running list of resolved decisions as we go, so nothing gets lost.

OUTPUT
- After each branch is resolved, summarize the confirmed behavior in one or
  two lines before moving to the next branch.
- At the end, produce a single Markdown file listing every resolved edge
  case as a checklist (`- [ ]` for ones still needing a code fix, `- [x]`
  for ones that are confirmed correct as-is), grouped by the six categories
  above.
