Solved issues:

- we can easily reuse whole tables using 'import'. (we could even automatically source their ref tables to make it easier)
- we can easily create building blocks of fields and import those.

Unsolved issues:

## How to elegantly select fields out of a collection?

Currently, we use tags for this. So we could reintroduce this as part of the import mechanism. E.g. import:path/File?tags=a,b

Alternatively, we could name the fields. E.g. import:path/File?field=b,a (would also set its order)
Or if better we could model it like
```
import:path/File
fields: b,c
```

## How to use subclassing while being able to set the order of fields properly

Currently, we define data model as flat list of columns that we assign to tables.
We could allow subclasses to be modelled in one file like so.
Then we limit inheritance but allow very precise definition of order.
Limitation is that order is same for all subclasses.

```
entity: Animals
variants:
- variant: Mammals
- variant: Dogs
  extends: Mammals
  # so we get all fields of variant Mammals too
fields:
- name: name
  key: 1
  # variant ommited defaults to Animals
- name: born alive
  type: boolean
  variant: Mammals
```

And then during import we could be precise what variants to implement
```
schema: my schema
entities:
- import: Animals?variant=Animals,Mammals
```