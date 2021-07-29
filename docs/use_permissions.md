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