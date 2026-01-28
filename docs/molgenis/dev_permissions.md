# Notes on permissions system

# Summary of Group Permissions Management Design

## 1. **Group Permissions**
- `group_permissions` table defines the permissions for groups on tables, with foreign key constraints to `group_metadata` and `table_metadata`.
- `group_permissions_trigger_function` manages granting and revoking permissions based on insertions, updates, and deletions in the `group_permissions` table.
- The function also ensures that the appropriate permission types are used when setting permissions for a schema.

## 2. **Materialized View for User Permissions**
- `user_permissions_mv` is a materialized view that caches the user permissions for fast lookup, which is refreshed when relevant changes happen in the `group_permissions` table.

## 3. **Row-Level Security (RLS)**
- The `enable_RLS_on_table` function enables RLS policies for tables in a schema.
- RLS policies are set up to manage permissions for different actions (SELECT, INSERT, UPDATE, DELETE) based on group membership and specific permissions defined in the system.
- `pg_has_role` and custom permission-checking functions (`has_permission`) are used in the RLS policies to control access.

## 4. **Default Permissions Setup**
- The `create_default_permissions` function sets up default schema-level permissions for predefined groups (`VIEWER`, `EDITOR`, `MANAGER`, `ADMIN`).
- This function creates the necessary roles and grants them appropriate permissions on schema objects and tables.