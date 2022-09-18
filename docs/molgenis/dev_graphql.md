# GraphQL in MOLGENIS

Each database in MOLGENIS has a graphql endpoint, that expose a graphql API for the data model of that database. In
addition, at the root there is a generic API.

For example:

* https://emx2.dev.molgenis.org/api/graphql - root api
* https://emx2.dev.molgenis.org/pet%20store/api/graphql - api for database 'pet store'

Full documentation can be found while visiting the graphql-playground app. You can click 'docs' there.

* https://emx2.dev.molgenis.org/apps/graphql-playground/ - playground for 'root' api
* https://emx2.dev.molgenis.org/pet%20store/graphql-playground/ - example for 'pet store' database

## Functions available on all APIs.

These functionalities are available for both the 'root' api and the database api.

### Sign in

Sign into an existing account. When running in a website then a session will be created. However, when you use a script
you can retrieve a token to authenticate in other calls. Provide these in subsequent request headers as '
x-molgenis-token'.

```graphql
mutation {
    signin(password: "bofke", email: "bofke@some.org") {
        message
        token
    }
}
```

### Sign up

```graphql
mutation {
    signup(email: "bofke@some.org", password: "somepass") {
        message
    }
}
```

### Sign out

```graphql
mutation {
    signout {
        message
    }
}
```

### changePassword

```graphql
mutation {
    changePassword(password: "password")
}

```

### retrieve session object

In the session object you can get active account ('email'), the roles of this user, and a listing of the schema's in the
system.

```graphql
{
    _session {
        email
        roles
        schemas
    }
}
```

### retrieve settings

MOLGENIS has a generic key/value settings for storing settings. Settings can be set on level of whole system, and per
database (per database includes the overal settings).

```graphql
{
    _settings {
        key
        value
    }
}
```

## Functions available for each database

### query schema

```graphql
{
    _schema {
        name
        tables {
            name
            id
            externalSchema
            inherit
            tableType
            description
            columns {
                name
                id
                description
                position
                columnType
                key
                required
                refTable
                refLink
                refBack
                refLabel
                validation
                visible
                semantics
            }
            settings {
                key
                value
                table
            }
            semantics
        }
        members {
            email
            role
        }
        roles {
            name
        }
    }
}
```

## change schema elements

You can change objects from schema query above and then pass them into the change function.

```graphql
mutation{
    change(
        tables: [...],
        members: [...]
        settings: [...]
        columns: [...]
    ){
        message
    }
}
```

## drop/remove schema elements

Note that settings can be on level of schema, or level of tables. In that later case you need to provide the table as
well.

```graphql
mutation{
    drop(
        tables: ["table1","table2"],
        members: ["email1","email2"],
        settings: [{key: "key1"},{key:"key2",table:"table1"}]
        columns: [{table: "table1", column: "column1"}]
    ){
        message
    }
}
```

## Example functions that will be available for each table in the database

Finally, for each table there are the following functions:

- query (has the name of the table)
- insert - to add rows
- update - to update rows
- save - to insert or if exist update rows
- delete - to remove rows

### query example

Simple query, including count

```graphql
{
    Pet {
        name
        category {
            name
        }
        tags {
            name
        }
    }
    Pet_agg {
        count
    }
}
```

Query including search

```graphql
{
    Pet(search:"poo") {
        name
        category {
            name
        }
        tags {
            name
        }
    }
}
```

Query using filters, limit, offset. Note that filter enables quite complex queries using _or and _and operators.

```graphql
{
    Pet(limit: 1, offset: 0, filter: { name: { equals: "pooky" }, weight: {between:[1,100]} }) {
        name
        category {
            name
        }
        tags {
            name
        }
        weight
    }
}
```

### mutation example

Every mutation accepts the same object that you can retrieve via queries. This makes it easy to implement retrieve, and
then update mutations.

example insert

```graphql
mutation {
    insert(Pet: { name: "mickey", category: { name: "cat" }, weight: 10.0 }) {
        message
    }
}
```

update, save, delete work exactly the same.

## Implementation hints

Below some implentation hints

### Javascript

Note that there are many javascript libraries to ease this process. We here use an example using a
simple [graphql-request](https://github.com/prisma-labs/graphql-request) library. When using javascript, you can use
wildcards to pass a json object in combintation with your query. Example:

```javascript
//untested
import {request} from 'graphql-request'

let row = {name: "mickey", category: {name: "cat"}}
let query = `mutation insert($row: [PetInput]) {
  insert(Pet: $row) {
    message
  }
}`

client.request(query, {Pet: row}).then((data) => console.log(data)).catch((error) => console.log(error));
```

### Example querying the Pet Store

Go to the [Pet Store playground](https://emx2.dev.molgenis.org/pet%20store/graphql-playground/).

Get the name of all the pets

```
{
  Pet {
    name
  }
}
```

<small>Tip: if you use ctrl + space inside the curlybraces of, in this case, Pet, you get autocomplete on its properties.</small>


Get only the pet named Pooky

```
{
  Pet(filter: { name: { equals: "pooky" } }){
    name,
    status,
    weight,
    photoUrls
  }
}
```

Get all the pets that have the letter K in them

```
{
  Pet(filter: { name: { like: "k" } }){
    name,
    status,
    weight,
    photoUrls
  }
}
```

Get all the pets that have the letter k and are sold

```
{
  Pet(filter: { name: { like: "k" }, _and: { status: { like: "sold" } } } ) {
    name,
    status,
    weight,
    photoUrls
  }
}
```

# Developing 'apps'

When you deploy an 'app' (see https://github.com/molgenis/molgenis-emx2/tree/master/apps)
* You will find a 'graphql' endpoint automatically served within the root of your app so to easy program against it
* In case of serving app in a schema, you will get 'schema' graphql endpoint, e.g. https://emx2.dev.molgenis.org/pet%20store/tables/
* In case of serving the app in 'central' you will get 'database' graphql endpoint, e.g. https://emx2.dev.molgenis.org/apps/central/
