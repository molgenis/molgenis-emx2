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

### To enable OIDC the following environment variables need to be set:

```
MOLGENIS_OIDC_CLIENT_ID // the id for the molgenis instance as set in the authentication provider
MOLGENIS_OIDC_CLIENT_SECRET // the client secret as set in the authentication provider
MOLGENIS_OIDC_CLIENT_NAME // the client name as set in the  authentication provider, defaults to MolgenisAuth
MOLGENIS_OIDC_DISCOVERY_URI // location of authentication provider (with path to relevant service)
MOLGENIS_OIDC_CALLBACK_URL // public available endpoint for molgenis service to handle the login action ( https://[public server location]/_callback, note the '_callback' is added by the molgenis server )
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

### FAQ: hints to setup keycloak

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
