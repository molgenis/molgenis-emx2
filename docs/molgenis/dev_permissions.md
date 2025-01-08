# Notes on permissions system

# Summary of Group Permissions Management Design

## 1. **Group Metadata and Roles**
- `group_metadata` table manages the groups, with triggers to handle the creation, updating, and deletion of roles associated with the groups.
- The `group_metadata_trigger_function` manages the creation, renaming, and deletion of PostgreSQL roles based on changes in the `group_metadata` table.

## 2. **Group-User Mapping**
- `group_users` table maps users to groups with foreign key constraints.
- `group_user_permissions_trigger_function` manages permissions granted or revoked when users are added or removed from groups.

## 3. **Group Permissions**
- `group_permissions` table defines the permissions for groups on tables, with foreign key constraints to `group_metadata` and `table_metadata`.
- `group_permissions_trigger_function` manages granting and revoking permissions based on insertions, updates, and deletions in the `group_permissions` table.
- The function also ensures that the appropriate permission types are used when setting permissions for a schema (`'_ALL_'` entries allow certain permission types like `'ADMIN'`).

## 4. **Materialized View for User Permissions**
- `user_permissions_mv` is a materialized view that caches the user permissions for fast lookup, which is refreshed when relevant changes happen in the `group_permissions` table.

## 5. **Role Grants and Refresh**
- An event trigger (`refresh_on_role_grant`) is set up to refresh the materialized view whenever a role (`MG_PERM`) is granted.
- `refresh_user_permissions_mv` refreshes the materialized view after insert, update, or delete on the `group_permissions` table.
- `refresh_user_permissions_on_role_grant` function ensures the materialized view is refreshed when the relevant roles are granted.

## 6. **Row-Level Security (RLS)**
- The `enable_RLS_on_table` function enables RLS policies for tables in a schema.
- RLS policies are set up to manage permissions for different actions (SELECT, INSERT, UPDATE, DELETE) based on group membership and specific permissions defined in the system.
- `pg_has_role` and custom permission-checking functions (`has_permission`) are used in the RLS policies to control access.

## 7. **Default Permissions Setup**
- The `create_default_permissions` function sets up default schema-level permissions for predefined groups (`VIEWER`, `EDITOR`, `MANAGER`, `ADMIN`).
- This function creates the necessary roles and grants them appropriate permissions on schema objects and tables.