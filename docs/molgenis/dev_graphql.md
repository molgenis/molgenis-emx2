# GraphQL in MOLGENIS

TODO, describe basics. Meanwhile, please use self-documenting GraphQL IDE inside MOLGENIS.

# Getting started 

* Introductionary information on graphql standard can be found at https://graphql.org/
* Each 'schema' has a graphql API: 
  * This can be accessed remotely using http://servername/schemaname/graphql
  * Or using playground using http://servername/schemaname/graphql-playground
* In addition there is a 'database' level graphql API:
  * This can be accessed remotely using http://servername/api/graphql
  * Or using playground using http://servername/schemaname/apps/graphql-playground/
  
In most schemas on top side of the menu you also see a link to _GraphQL_ or _GraphQL API_
this will open up a GraphQL playground for the selected schema (or in case of 'central' the database level API. <br>

For example:
* Database level API at https://emx2.dev.molgenis.org/api/graphql 
* Databaswe level playground at https://emx2.dev.molgenis.org/apps/graphql-playground
* Schema level API for 'pet store' at https://emx2.dev.molgenis.org/pet store/graphql 
* Schema level playground for 'pet store' at https://emx2.dev.molgenis.org/pet store/graphql-playground 

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
