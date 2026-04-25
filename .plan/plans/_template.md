# Plan: <title>

## Goal
<one sentence>

## Phase <N>: <phase name>

### Story <N.M>: <story name>

**Test cadence:** RED-per-story | RED-batched-at-phase-end

- Default for breaking changes (renames, enum reshapes, interface migrations, anything cross-module) -> RED-per-story
- Default for additive features (new fields, new endpoints, isolated logic) -> RED-batched-at-phase-end
- When in doubt -> RED-per-story

**Module:** `backend/<module>`
**Acceptance criteria:**
- [ ] <criterion>

## Open questions
- <question>
