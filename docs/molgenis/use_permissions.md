# Permissions

## Standard roles

Currently we have as standard roles:

* manager - context: schema. Has permissions to assign roles to users in its schema. Can create/change/remove tables and
  columns (i.e. change the schema)
* editor - context: schema. Has permission to insert, update, delete rows in tables. Can NOT change schema.
* viewer - context: schema. Has permission to view data.

## Users can get roles in a schema

Access to databases is controlled by providing roles to users. A user with a role we call a 'member' of a
database/schema. See: [database settings](use_database_settings.md).

In addition there are two special users:

* admin - context: whole database. Has rights to view and edit everything.
* anonymous - context: whole database. Is any user that has not signed in. You can give 'view' permission to 'anonymous'
  user (i.e, make anonymous member of your schema)

## Users can be managed by admin

Individuals can 'sign up' to register themselves to MOLGENIS databases, choosing user name and password. Special user is
the 'admin'. Only this user can see and create other users.

## Sign-in using Open ID Connect (OIDC)

Users can be authenticated using an existing account via Open ID Connect (OIDC).
To enable users to sign-in using OIDC, the Molgenis instance must be configured to use OIDC.
When OIDC is enabled, users are no longer presented with the option to sign-up

When OIDC is enabled, the admin user can bypass the oidc login by using the admin route (```[service-location]/apps/central/#/admin```)

### Enabling OIDC
To enable OIDC the following environment variables need to be set:
```
MOLGENIS_OIDC_CLIENT_ID // the id for the molgenis instance as set in the authentication provider
MOLGENIS_OIDC_CLIENT_SECRET // the client secret as set in the authentication provider
MOLGENIS_OIDC_CLIENT_NAME // the client name as set in the  authentication provider, defaults to MolgenisAuth
MOLGENIS_OIDC_DISCOVERY_URI // location of authentication provider (with path to relevant service)
MOLGENIS_OIDC_CALLBACK_URL // public available endpoint for molgenis service to handle the login action ( https://[public server location]/_calback )
```

### Disabling OIDC
Remove the ```MOLGENIS_OIDC_CLIENT_ID``` environment variable and restart the server