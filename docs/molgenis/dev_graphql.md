# GraphQL in MOLGENIS

TODO, describe basics. Meanwhile, please use self-documenting GraphQL IDE inside MOLGENIS.

# Getting started 

## GraphQL API Playground

In the top side of the menu you see a link to _GraphQL_ or _GraphQL API_
this will open up a GraphQL playground for the selected database. <br>
So that means, given the following url ```https://emx2.dev.molgenis.org/apps/central/#/ ``` <br>
**apps** is the selected database. <br>

And for example ```https://emx2.dev.molgenis.org/pet%20store/tables/#/``` gives access to the pet store when clicking on the link.

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