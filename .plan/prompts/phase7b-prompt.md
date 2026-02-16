# Phase 7b Prompt — Admin UI Permission Matrix

Paste this as your first message in a fresh Claude Code context.

---

You are the lead agent. Implement Phase 7b: Admin UI Permission Matrix for fine-grained permissions.

## Context

Read these files first:
- `.plan/plans/phase7b-admin-ui.md` — detailed implementation plan
- `.plan/plans/finegrained-permissions.md` — overall project plan (phases 1-6 are DONE)
- `.plan/specs/finegrained-spec.md` — technical spec

The backend GraphQL API for roles and permissions is fully implemented and tested. You are building the frontend UI.

## What to build

A "Roles" page at `/[schema]/roles` in `/apps/ui` that shows a permission matrix for managing custom roles. See the plan for full details.

## Approach

Follow CLAUDE.md workflow strictly:
1. Read the plan files above
2. Study existing patterns: `apps/ui/app/pages/admin/users.vue` (740 lines, best reference for admin CRUD patterns), `apps/ui/app/util/adminUtils.ts` (GraphQL query patterns), `apps/ui/app/pages/[schema]/index.vue` (schema page patterns)
3. Study tailwind-components: Table/TableRow/TableCell, Button, Modal, Select, ContentBlock, PageHeader, Container, Tab, BreadCrumbs (all in `apps/tailwind-components/app/components/`)
4. Implement in order: roleUtils.ts → PermissionCell.vue → PermissionMatrix.vue → RoleEditor.vue → roles.vue page → navigation wiring
5. Use subagents: spawn frontend agent for implementation, review agent for quality check
6. Run `pnpm test` and `pnpm lint` in `apps/ui` to verify
7. For visual testing: start backend with `./gradlew run` (from repo root, background, takes ~60s) and frontend with `pnpm dev` in `apps/ui` (background), then use playwright to verify
8. Stage all changes with `git add`, never commit

## Key technical details

- GraphQL endpoint for schema queries: `POST /${schemaId}/graphql`
- Roles query: `{ _schema { roles { name, description, system, permissions { table, select, insert, update, delete, grant, columns { editable, readonly, hidden } } } } }`
- Tables query (combine with roles): `{ _schema { tables { id, label, tableType } } }`
- Change mutation: `mutation { change(roles: [{name: "...", permissions: [...]}]) { message } }`
- Drop role: `mutation { drop(roles: ["..."]) { message } }`
- Drop permission: `mutation { drop(permissions: [{role: "...", table: "..."}]) { message } }`
- SELECT levels: EXISTS, RANGE, AGGREGATOR, COUNT, TABLE, ROW
- INSERT/UPDATE/DELETE levels: TABLE, ROW
- GRANT: boolean (true/false)
- `*` table means schema-wide wildcard
- System roles are read-only in the UI
- Access restricted to Manager+ (use `_session` to check)
- Use `$fetch` for GraphQL calls (same pattern as adminUtils.ts)
- Import tailwind-components via relative path `../../../../tailwind-components/app/components/...` (same as other pages)

## Quality rules
- No comments in code
- No single-character variable names (except i,j,k iterators)
- Follow existing patterns exactly
- Stage changes with `git add` when done
- Run `pnpm test` and `pnpm format && pnpm lint` in `apps/ui` before reporting done
