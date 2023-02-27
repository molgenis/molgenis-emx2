# core concepts

## archetypes

Archetypes are definitions tables thatsof which the intentions is that whole or
parts can be reused. Each file in archetypes declares the structure of
***one*** table.

## instances

Instances are definitions of actual schemas we use(d) in applications. With
schemas we mean complete set of tables, their columns and their relationships
such that it can be deployed in for example MOLGENIS emx2.

Typically, instances reuse (parts of) archetypes, but they don't have too.

## standards

# YAML reference

## schema

| name | type | description |
|------|------|-------------|
| description | text | description of the schema |
| version | string | version number |
| prefixes | map    | for rdf definitions, key,value map of all prefixes |
| tables | list(table) | list of table defintions |

## table

| name | type         | description | 
|------|--------------|-------------|
| name | string       | name of the table |
| prefixes | map    | for rdf definitions, key,value map of all prefixes |
| fields | list(column) | list of column definitions |

## column

| name | type       | description |
|------|------------|-------------|
| name | string     | name unique in table |
| key | integer    | when a field is unique you can add it to a key |
| type | type_value | type definitionn conform emx2 types |

## type_value

Valid type values are:

- string
- ...todo

# design  background

## inspiration

* emx2
* emx1
* fair-genomes
* unified model
* bioschemas.org
* linkml
* json-schema

## requirements

* MUST can include columns from archtetype
    * importantly, if desired at particular position in list of columns
* MIGHT can omit columns from archteypte
* MUST can override columns from archetype in non-breaking way
    * change non-breaking details such as label, description
    * restrict type such subsetting (type=date instead of datetime, use a subset
      of ontology)
    * restrict constraingts ()
    * can NOT change type, or remove constraints.
* MIGHT be able to define foreign keys to be of more than one other table
  (as replacement of the inheritance we now have)

## design proposals

* don't use inheritance, instead we use a mixin like 'include' approach to
  include (parts of) other tables in your current table
    * complications: how to do foreign keys in those cases? previously there
      were ways to query the differnt supertypes??