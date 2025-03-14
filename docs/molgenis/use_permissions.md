# Group and permission system

MOLGENIS provides fine-grained access control using the following components:

* **Users**: Individual accounts that interact with the system.
* **Groups**: Collections of users sharing permissions.
* **Permissions**: Rules defining the actions users of a group can perform.

Permissions can apply to individual tables or entire schemas.

## permissions
Permissions define what actions a user can do.

| Permission       | Definition                                                                                                     |
|------------------|----------------------------------------------------------------------------------------------------------------|
| select           | Allows selecting all rows.                                                                                     |
| insert           | Allows creating new rows. <br/>If a user belongs to multiple groups, they must specify the group for the new row. |
| update           | Allows updating all rows, including the 'group' field.                                                         |                                                                               |
| delete           | Allows deleting all rows.                                                                                      |                                                                                            |
| group_select     | Restricts ```select``` to rows in groups user is member of.                                                    |
| group_update     | Restricts ```update``` to rows in groups user is member of. Does not permit changing the row's group.          |                                     |
| group_delete     | Restricts ```delete``` to rows in groups user is member of.                                                    |
| aggregate        | Allows running group-by queries* to calculate aggregated values (e.g., counts, sums, averages).                |
| aggregate_exist  | Restricts ```aggregate``` to "yes/no" responses                                                                |
| aggregate_range  | Restricts ```aggregate``` to counts returned in increments of 10 (e.g., 10, 20, ... 130).                      |
| aggregate_count  | Restricts ```aggregate``` to counts only.                                                                      |
| admin_metadata   | Allows creating, altering, and dropping tables.                                                                |
| admin_permission | Allows granting and revoking permissions for groups.                                                           |
| admin_group      | Allows adding and removing users from groups.                                                                  |

**Note on Group-By Queries**: Group-by queries are limited to filtering on ref, ref_array, ontology, or ontology_array fields. To prevent potential identification risks, additional safeguards (e.g., minimum thresholds for counts) may apply.

## default groups per schema

MOLGENIS creates following default groups for each schema:

| default group | permissions                                                                                                    |
|---------------|----------------------------------------------------------------------------------------------------------------|
| viewers       | Allows ```select``` on all tables.                                                                             |
| group_viewers | Restricts ```viewers``` to ```group_select``` on **data** tables and select on all **ontology** tables         |
| editors       | Extends ```viewers``` by adding ```insert```, ```update```, ```delete``` on all tables.                        |
| group_editors | Restricts ```editors``` to ```group_insert```, ```group_update```, ```group_delete``` on  **data** tables only |
| managers      | Extends```editors``` by adding ```admin_group_members``` (allows managing group memberships).                  |
| admins        | Extends ```managers``` by adding ```admin_metadata``` and ```admin_group_permissions```  (enables schema-level administration).                       |

**Note on ```data``` and ```ontology``` tables**:
* Data Tables: Contain sensitive user or entity-related data. Restrictions such as ```group_select```, ```group_update```, and ```group_delete``` apply here..
* Ontology Tables: Contain non-sensitive reference data (e.g., code lists). These are typically accessed with unrestricted ```select``` permissions.

## Custom groups

In addition to the default groups, MOLGENIS allows administrators to define custom groups tailored to specific use cases. Custom groups can be configured with any combination of schema-level and table-level permissions.

Features of Custom Groups:
* **Granular Permissions**: Combine permissions like select, group_select, update, group_update, etc., to suit specific needs.
* **Schema-Level Permissions**: Apply permissions across an entire schema, streamlining access control for groups working on multiple tables.
* **Table-Level Permissions**: Restrict permissions to specific tables for precise data management.

## Examples

Here’s an example of how a "group editor" might interact with a data table:
* Scenario: A user is part of a "research_team" group.
* Permissions: The user has group_update on a samples table.
* Result: The user can update rows in the samples table only if those rows are assigned to the research_team group.

## Implementation notes

n.b. we use postgresql roles to implement all this:
- a 'group' is a role that has no password and that is only meant as intermediate between users and permissions.
- a 'permission' is a role that has no password and that has relevant grants (select, update, delete, create, alter, drop, grant) and which is also used to check in row level security policies


## Users can become member of one or more group

Access to databases is controlled by adding users to groups. A user with a role we call a 'member' of a
database/schema. See: [database settings](use_database_settings.md).

In addition there are three special users:
* anonymous - context: whole database. Is any user that has not signed in. You can give 'view' permission to 'anonymous'
  user (i.e, make anonymous member of your schema)
* user - context: whole database. Is any user that has signed in. E.g. you can give 'edit' permission to 'user' to say that all authenticated users are authorized to edit
* admin - context: whole database. Has rights to view and edit everything (cannot be changed).

## Users can be managed by admin

Individuals can 'sign up' to register themselves to MOLGENIS databases, choosing user name and password. Special user is
the 'admin'. Only this user can see and create other users.

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
