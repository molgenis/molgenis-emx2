# Architecture notes

EMX2 consists of a 'server' and a 'client' part. The server code is written in java and can be found in 'backend'. The
client code is written in vue and can be found in 'apps'. Most interaction between client and server uses GraphQL.

## Uses postgresql schemas

The core of EMX2 is that data is organised using Postgresql schemas. Each schema has the roles 'manager', 'editor', 'viewer','aggregator'.
Each schema is served on its own path. E.g. http://localhost:8080/pet%20store

## Graphql is automatically served per schema

EMX2 creates a graphql endpoint per schema. Depending on user permissions (you probably need to signin) you get more or
less detail in the schema. You can signin/signup/signout via graphql.

## Apps are automatically served per schema

To make app development easy each app is automatically served on each schema.
E.g. http://localhost:8080/pet%20store/tables/ will serve app in /apps/tables/ (don't forget trailing /)
Then within your app, you will have graphql simply at 'graphql' path. This makes it easy to have apps run in different
contexts.

## Each schema has settings endpoint

To ease creation of user settings, each schema has _settings endpoint in grahpq that can be edited by users with the manager
role. This is simply a hashmap of the form:

```
_settings:[
{key: 'name', value:'some value'}
]
```

Each app can decide what settings to implement. Obviously, you should namespace settings to prevent name conflicts. Good
practice is to use 'appname'.'key' as key names. Current examples of settings are 'menu' (for changing links in the
menu, value is a json string), 'cssURL' (for changing the bootstrap link to another css file), 'page' (for creating
custom pages).

## Reusable 'Molgenis' component as context

To ease app development there is a 'Molgenis' wrapper vue component that you can use as a starting point for your apps.
This component provides menu, css url and signin/out buttons.

## Files

Files are stored within postgresql as blob. This given that experience learns that most file data is small. We expect
that if you have big files you will simply link to them.
