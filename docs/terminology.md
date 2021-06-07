# Terminology

## Databases with schemas

MOLGENIS enable creation of multiple databases on one server. Technically these are postgresql schemas, hence we use '
database' and 'schema' interchangeably. A database is defined as a collection of tables (i.e. schema). In addition each
database can have settings, members, menu and theme.

## Schema consists of tables

The contents of each molgenis database is defined as tables. A table is defined by its columns (i.e. metadata) and
contains data in rows. The metadata describing the tables and columns we call 'schema'.

## Users can become members

Access to databases is controlled by providing roles to users. A user with a role we call a 'member' of a
database/schema. See: database settings. Currently we have as standard roles:

* manager - context: schema. Has permissions to assign roles to users in its schema. Can create/change/remove tables and
  columns (i.e. change the schema)
* editor - context: schema. Has permission to insert, update, delete rows in tables. Can NOT change schema.
* viewer - context: schema. Has permission to view data.

In addition there are two special users:

* admin - context: whole database. Has rights to view and edit everything.
* anonymous - context: whole database. Is any user that has not signed in. You can give 'view' permission to 'anonymous'
  user (i.e, make anonymous member of your schema)

## Users can be managed by admin

Individuals can 'sign up' to register themselves to MOLGENIS databases, choosing user name and password. Special user is
the 'admin'. Only this user can see and create other users.