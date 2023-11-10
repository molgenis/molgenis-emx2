# GraphQL in MOLGENIS

Each database in MOLGENIS has a graphql endpoint, that expose a graphql API for the data model of that database. In
addition, at the root there is a generic API.

For example:

- https://emx2.dev.molgenis.org/api/graphql - root api
- https://emx2.dev.molgenis.org/pet%20store/api/graphql - api for database 'pet store'

Full documentation can be found while visiting the graphql-playground app. You can click 'docs' there.

- https://emx2.dev.molgenis.org/apps/graphql-playground/ - playground for 'root' api
- https://emx2.dev.molgenis.org/pet%20store/graphql-playground/ - example for 'pet store' database

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

### createToken

Create a token

```graphql
mutation {
  createToken(email: "admin", tokenName: "mytoken") {
    token
    message
  }
}
```

See tokens for current user

```graphql
{
  _session {
    settings {
      key
      value
    }
  }
}
```

See tokens for all users, in settings 'access-tokens'

```graphql
{
  _admin {
    users {
      email
      settings {
        key
        value
      }
    }
  }
}
```

### session object

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

### settings

MOLGENIS has a generic key/value settings query for storing settings on database level

```graphql
{
  _settings {
    key
    value
  }
}
```

On level of schema, tables you can follow similar queries

```graphql
{
  _schema {
    settings {
      key
      value
    }
    tables {
      settings {
        key
        value
      }
    }
  }
}
```

For current user you can query settings following same pattern

```graphql
{
  _session {
    settings {
      key
      value
    }
  }
}
```

To get settings for all users

```graphql
{
  _admin {
    users {
      email
      settings {
        key
        value
      }
    }
  }
}
```

To change settings you can use the 'change' mutation, e.g.

Database or schema settings (depending on database vs schema api):

```graphql
mutation {
  change(settings: [{ key: "key", value: "value" }]) {
    message
  }
}
```

Table settings:

```graphql
mutation {
  change(
    tables: [{ name: "mytable", settings: [{ key: "key", value: "value" }] }]
  ) {
    message
  }
}
```

User settings (only as admin, or settings of current user):

```graphql
mutation {
  change(
    users: [{ email: "bofke", settings: [{ key: "key", value: "value" }] }]
  ) {
    message
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
      schemaId
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
mutation {
  drop(
    tables: ["table1", "table2"]
    members: ["email1", "email2"]
    settings: [{ key: "key1" }, { key: "key2", table: "table1" }]
    columns: [{ table: "table1", column: "column1" }]
  ) {
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
  Pet(search: "poo") {
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

Query using filters, limit, offset. Note that filter enables quite complex queries using \_or and \_and operators.

```graphql
{
  Pet(
    limit: 1
    offset: 0
    filter: { name: { equals: "pooky" }, weight: { between: [1, 100] } }
  ) {
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
import { request } from "graphql-request";

const row = { name: "mickey", category: { name: "cat" } };
let query = `mutation insert($row: [PetInput]) {
  insert(Pet: $row) {
    message
  }
}`;

client
  .request(query, { Pet: row })
  .then((data) => console.log(data))
  .catch((error) => console.log(error));
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

You can also filter the subsets in your result.
Given the pet Spike in the petstore, he has two tags:

```
{
  Pet(filter: {name: {equals: "spike"}}) {
    name,
    tags {
      name
    }
  }
}
```

Results in:

```
{
  "data": {
    "Pet": [
      {
        "name": "spike",
        "tags": [
          {
            "name": "red"
          },
          {
            "name": "green"
          }
        ]
      }
    ]
  }
}
```

If you only want to have the green tag in your result, you can also apply a filter on tags.

example:

```
{
  Pet(filter: {name: {equals: "spike"}}) {
    name,
    tags(filter: {name: {equals: "green"}}) {
      name
    }
  }
}
```

Will return:

```
{
  "data": {
    "Pet": [
      {
        "name": "spike",
        "tags": [
          {
            "name": "green"
          }
        ]
      }
    ]
  }
}
```

# Developing 'apps'

When you deploy an 'app' (see https://github.com/molgenis/molgenis-emx2/tree/master/apps)

- You will find a 'graphql' endpoint automatically served within the root of your app so to easy program against it
- In case of serving app in a schema, you will get 'schema' graphql endpoint, e.g. https://emx2.dev.molgenis.org/pet%20store/tables/
- In case of serving the app in 'central' you will get 'database' graphql endpoint, e.g. https://emx2.dev.molgenis.org/apps/central/

<br />
<br />

# GraphQL JavaScript Library (Query EMX2)

Inside the molgenis-components library there is library to make
querying emx2 with graphQL a breeze.

## Installation

Given the fact that you have an application under the app folder like so:

`%root%/apps/%myApp%`
(substitute myApp / root for the correct paths)

Go to the `package.json` inside the `%myApp%` folder.

add the following to the dependancies section

```
  "dependencies": {
    ...other things you might have,
    "molgenis-components": "*"
  }
```

Then open a terminal and type in `yarn`.

> sometimes you may need to build molgenis-components locally. Go into the molgenis-components folder inside the apps directory. Open a terminal and type `yarn build`

Now you can import the library as follows:

`import { QueryEMX2 } from "molgenis-components";`

Now you have access to the QueryEMX2 class!

## Usage

Create a new connection:

`const query = new QueryEMX2('graphQLEndpoint')`

Where graphQLEndpoint is the endpoint of the api. Usually `'graphql'`

The way it works is with _function chaining_.

To proceed we have to specify a table:

`query.table('MyTable')`

Add a selection, this can either be an array of strings or a single string, or blank to get everything.

`query.select(["id","name'])`

You can even query nested properties using the _dot_ notation, just like how you would access a JSON object.

`query.select(["id", "name", "refTable.id", "refTable.name"])`

to get the results:

`const results = await query.execute()`

> For debugging purposes you can use `query.getQuery()` to see how to graphQL query has been formed.

### Filters

When you specify a table using `.table("MyTable")` MyTable is in the case of QueryEMX2 seen as 'root' table.

When you want to filter on the _root_ table you can use the `.where()` function for an _and_ clause and `orWhere()` function for a _or_ clause.

Then you combine that with an operator function that takes the filter value as an argument.

the following function are available:

- find(value)
- search(value)
- equals(value)
- in(value) / orLike(value)
  /\*_ custom type, to make it into a bracket type query: { like: ["red", "green"] } _/
- like(value)
- notLike(value)
- triagramSearch(value)
- textSearch(value)
- between(value)
- notBetween(value)

If you want to filter a ref/mref/categorial or any other 'nested' table result, use:

`filter` for _and_ clause <br />
`orFilter` for _or_ clause

which will apply the filters on that table.

## Examples

**Multiple or Query**

```
const query = new QueryEMX2("graphql")
      .table("Biobanks")
      .select(["id", "name"])
      .where("name")
      .like("Dresden")
      .orWhere("country.name")
      .like("DE")
      .orWhere("collections.name")
      .like("covid")
      .orWhere("collections.materials.name")
      .like("covid")
      .getQuery();
```

Output:

```
{
Biobanks(filter: { _and: [ { name: { like: "Dresden" } } ], _or: [ { country: { name: { like: "DE" } } }, { collections: { name: { like: "covid" } } }, { collections: { materials: { name: { like: "covid" } } } } ] }) {
    id,
    name
  }
}
```

**Nested Query**

```
const basic = ["id", "name"];
const selection = [
  ...basic,
  {
    LayerA: [
      ...basic,
      {
        layerB: [
          ...basic,
          {
            layerC: [...basic],
          },
        ],
      },
    ],
  },
];

const query = new QueryEMX2("graphql")
  .table("NestedExample")
  .select(selection)
  .filter("layerC.name")
  .like("nameOfC")
  .getQuery();
```

Output:

```
{
NestedExample {
    id,
    name,
    layerA {
        id,
        name,
        layerB {
            id,
            name,
            layerC(filter: { _and: [ { name: { like: "nameOfC" } } ] }) {
                id,
                name
            }
        }
    }
  }
}
```

Limit, orderBy

```
    const query = new QueryEMX2("graphql")
      .table("Biobanks")
      .select(["id", "name"])
      .where("name")
      .like("UMC")
      .limit("biobanks", 100)
      .orderBy("biobanks", "name", "asc")
      .getQuery();
```

Output:

```
{
Biobanks(limit: 100, orderby: { name: ASC }, filter: { _and: [ { name: { like: "UMC" } } ] }) {
    id,
    name
  }
}
```

Filter on nested properties

```
const query = new QueryEMX2("graphql")
  .table("Biobanks")
  .select(["id", "name"])
  .where("collections.id")
  .like("eric")
  .where("collections.name")
  .like("Lifelines")
  .getQuery();
```

Output:

```
{
Biobanks(filter: { _and: [ { collections: { id: { like: "eric" } } }, { collections: { name: { like: "Lifelines" } } } ] }) {
    id,
    name
  }
}

```
