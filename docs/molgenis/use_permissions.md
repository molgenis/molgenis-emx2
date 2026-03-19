# Permissions

## Standard roles

Currently we have as standard roles:

* **manager** - context: schema. Has permissions to assign roles to users in its schema. Can create/change/remove tables and
  columns (i.e. change the schema)
* **editor** - context: schema. Has permission to insert, update, delete rows in tables. Can NOT change schema.
* **viewer** - context: schema. Has permission to view data.
* **aggregator** - context: schema. Has permission to count table rows (unless <10), and to view ontology data

In addition we have special roles to allow for specific permissions around aggregation which currently only have effect on graphql and beacon APIs"

* **count** - context: schema. Has permission to count table rows, and to view ontology data
* **range** - context: schema: Has permission to count table rows, with a step-size of 10 (e.g. 10,20..120,130 etc.), and to view ontology data
* **exists** - context: schema: Has permission to see if data exists given certain filters, and to view ontology data


## Users can get roles in a schema

Access to databases is controlled by providing roles to users. A user with a role we call a 'member' of a
database/schema. See: [database settings](use_database_settings.md).

In addition there are three special users:
* **anonymous** - context: whole database. Is any user that has not signed in. You can give 'view' permission to 'anonymous'
  user (i.e, make anonymous member of your schema)
* **user** - context: whole database. Is any user that has signed in. E.g. you can give 'edit' permission to 'user' to say that all authenticated users are authorized to edit
* **admin** - context: whole database. Has rights to view and edit everything (cannot be changed).


## Users can be managed by admin

Individuals can 'sign up' to register themselves to MOLGENIS databases, choosing username and password. Special user is
the 'admin'. Only this user can see and create other users.

## Admin rights can be delegated
Root admin user can grant admin rights to other users. These users can perform administrative tasks across the system, similar to the root admin.

Details:
* Only the root admin can grant or revoke admin rights.
* A user with admin status has full access to manage schemas, users, and permissions across the system.
* Users with admin status cannot change the root admin's password or remove the root admin account.

## Custom roles

In addition to the standard roles, users with Manager or Owner access can create custom roles with fine-grained, per-table permission control.

Custom roles allow you to:
- Set different permission levels per table
- Use a wildcard (`*`) entry to define schema-wide defaults
- Override the wildcard with per-table entries to escalate or restrict access for specific tables

For example, a custom "Analyst" role might have TABLE-level SELECT on most tables (via the wildcard) but only COUNT-level SELECT on a sensitive table.

### Permission levels

**SELECT levels** control what data users can read, from most restrictive to least:

| Level | Description |
|-------|-------------|
| EXISTS | Can only check if data matching a filter exists (yes/no) |
| RANGE | Can count rows in ranges with step-size of 10 (e.g. 10, 20, 130) |
| AGGREGATOR | Can count rows (shows "<10" when count is below 10) |
| COUNT | Can count rows exactly |
| TABLE | Can read all rows and columns |
| ROW | Can read only rows tagged with the user's role (row-level security) |

**INSERT, UPDATE, DELETE levels** control what data users can modify:

| Level | Description |
|-------|-------------|
| TABLE | Can modify all rows in the table |
| ROW | Can only modify rows tagged with the user's role (row-level security) |

**GRANT** is a boolean flag. When enabled, the role can grant its own permissions to other users.

### Row-level security

When any permission is set to the **ROW** level, row-level security is activated for that table. This adds a `mg_roles` column to the table:

- On INSERT, `mg_roles` is automatically populated with the inserting user's role
- Users can only see/modify rows where `mg_roles` contains their active role
- Rows with `mg_roles` set to NULL are visible to all users with access to the table
- Multiple roles can be assigned to a single row (the column stores an array of role names)

Row-level security is enforced at the PostgreSQL level using Row-Level Security (RLS) policies, so it applies to all access methods (GraphQL, API, direct queries).

### Permission matrix UI

The permission matrix is available at `/{schema}/roles` for users with Manager or Owner access.

Using the permission matrix:
1. Select an existing role from the dropdown, or create a new role
2. The matrix shows one row per table, with columns for SELECT, INSERT, UPDATE, DELETE, and GRANT
3. The first row labeled `*` (wildcard) sets schema-wide defaults for all tables
4. Per-table rows override the wildcard — you can both escalate and restrict permissions
5. Inherited values (from the wildcard) are shown as placeholder text
6. Changes are saved when you click the save button
7. Use the URL query parameter `?role=RoleName` to bookmark a specific role view

### GraphQL API for roles

Roles and permissions can be managed programmatically via GraphQL.

**Query roles and permissions:**

```graphql
{
  _schema {
    roles {
      name
      description
      system
      permissions {
        table
        select
        insert
        update
        delete
        grant
      }
    }
  }
}
```

**Create or update a role with permissions:**

```graphql
mutation {
  change(roles: [{
    name: "Analyst"
    description: "Read-only with restricted access to sensitive tables"
    permissions: [
      { table: "*", select: "TABLE" }
      { table: "SensitiveData", select: "COUNT" }
    ]
  }]) {
    message
  }
}
```

**Delete a role:**

```graphql
mutation {
  drop(roles: ["Analyst"]) {
    message
  }
}
```

**Remove a specific table permission from a role:**

```graphql
mutation {
  drop(permissions: [{
    role: "Analyst"
    table: "SensitiveData"
  }]) {
    message
  }
}
```

### Examples

**Analyst role** — read all data, but only counts on a sensitive table:

| Table | SELECT | INSERT | UPDATE | DELETE |
|-------|--------|--------|--------|--------|
| `*` | TABLE | — | — | — |
| SensitiveData | COUNT | — | — | — |

**DataEntry role** — can insert and update but never delete:

| Table | SELECT | INSERT | UPDATE | DELETE |
|-------|--------|--------|--------|--------|
| `*` | TABLE | TABLE | TABLE | — |

**PetCurator role** — full access with grant on Pets table, read-only elsewhere:

| Table | SELECT | INSERT | UPDATE | DELETE | GRANT |
|-------|--------|--------|--------|--------|-------|
| `*` | TABLE | — | — | — | — |
| Pets | TABLE | TABLE | TABLE | TABLE | yes |

**ClinicNurse role** — row-level security on patient data:

| Table | SELECT | INSERT | UPDATE | DELETE |
|-------|--------|--------|--------|--------|
| `*` | TABLE | — | — | — |
| Patients | ROW | ROW | ROW | — |

With this setup, each nurse only sees and edits patient rows tagged with their role. New patients they add are automatically tagged.

# Sign-in using Open ID Connect (OIDC)

Users can be authenticated using an existing account via Open ID Connect (OIDC). To enable users to sign-in using OIDC,
the Molgenis instance must be configured to use OIDC. When OIDC is enabled, users are no longer presented with the
option to sign-up

When OIDC is enabled, the admin user can bypass the oidc login by using the admin
route (```[service-location]/apps/central/#/admin```)

## Enabling OIDC
MOLGENIS uses pac4j, see https://www.pac4j.org/docs/clients/openid-connect.html explaining some of the settings we expose.

### To enable OIDC the following environment variables need to be set:

```
MOLGENIS_OIDC_CLIENT_ID // the id for the molgenis instance as set in the authentication provider
MOLGENIS_OIDC_CLIENT_SECRET // the client secret as set in the authentication provider
MOLGENIS_OIDC_CLIENT_NAME // the client name as set in the  authentication provider, defaults to MolgenisAuth
MOLGENIS_OIDC_DISCOVERY_URI // location of authentication provider (with path to relevant service)
MOLGENIS_OIDC_CALLBACK_URL // public available endpoint for molgenis service to handle the login action ( https://[public server location]/_callback, note the '_callback' is added by the molgenis server )
```

Optionally:
```
MOLGENIS_OIDC_UNSIGNED_TOKEN // boolean indicating if unsigned tokens can be used, i.e. the 'none' algorithm

```

#### note: if oidc was previously disabled 
- signin to the emx2 server as Admin
- go to the settings app ( .../apps/central/#/admin/settings)
- and change the ```isOidcEnabled``` setting from ```false``` to ```true``` 


### The OIDC provider must return a valid ```email``` field ( also known as claim ) as part of the oidc profile response.

If a user with the given email is already known in the emx system the the oidc user will be logged in as this user. If
the email is not know a new user is created in the emx system ( with email provided oidc profile ) and the user is
signed in.

### Disabling OIDC

Remove the ```MOLGENIS_OIDC_CLIENT_ID``` environment variable and restart the server

### FAQ: hints to setup Keycloak and providers such as Life Science AAI (LS-AAI)

We learnt the following settings helped to get all running properly:

* install a 'keycloakserver'
* within keycloak, create a realm, e.g. 'myrealm'.
* within the realm, under 'login' set email as username
* within the realm, under authentication in required actions check 'configure OTP'(=MFA) 'verify email' as 'Default
  action'
* within the realm, setup a client with 'clientID'='myclientid' and set Access Type = 'confidential'.
* This will add a 'credentials tab' for your client containing a 'Secret' you need below
* probably within this client you also want to set Base url (so in keycloak it is linked as application for user)

See environment variables below:

```
MOLGENIS_OIDC_CLIENT_ID="myclientid" 
MOLGENIS_OIDC_CLIENT_SECRET="Secret"
MOLGENIS_OIDC_DISCOVERY_URI="http://keycloakserver/realms/myrealm/.well-known/openid-configuration"
MOLGENIS_OIDC_CALLBACK_URL="https://mymolgeniserver"
```
