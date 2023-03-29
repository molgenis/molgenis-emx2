# TODO list

* how to redirect references, potentially ability to link to the union of multiple tables
* do we want a similar method like 'extends' or 'is_a'
* how to best model standard compliance
* mapping hints, e.g. how fields should be nested in beacon/json
* how to merge, subset or extend ontologies
* unit (we want human readible but also onology unit, makes most sense to use ontology type for this)
* do we need ontolog on the relation? seehttps:
  //github.com/fairgenomes/fairgenomes-semantic-model/blob/84e685d3880dde067246f7fb6ea7fd72438d529c/fair-genomes.yml#L215
* LookupOne_NoGlobals?

# core concepts

## archetypes

Archetypes are definitions tables thatsof which the intentions is that whole or parts can be reused. Each file in archetypes declares the structure of
***one*** table.

## instances

Instances are definitions of actual schemas we use(d) in applications. With schemas we mean complete set of tables, their columns and their relationships such
that it can be deployed in for example MOLGENIS emx2.

Typically, instances reuse (parts of) archetypes, but they don't have too.

## standards

# YAML reference

## Files that define a table

These files are recognized by having a 'columns' key in the root. A directory with tables can be treated as a schema. Schema level metadata in _schema.yaml

```
name: Person                                        #A-Zaz0-9 and space (table name is required)
label: Person table                                 #string, can be authhing
description: Some example table for persons         #text, human readible
prefixes:                                           #key/value of prefixes, optional
  dct:   dct: http://purl.org/dc/terms/                  
columns:                                            #ordered map of columns
  #simple example
  identifier:                                       #column name, A-Zaz0-9 and space
    type: string                                    #type, default is string
    label: Unique Identifier                        #string, defaults to name of the column
    description: To identify each row               #text, default is empty
    key: 1                                          #numeric, default is empty
    required: true                                  #boolean, default is false
    readonly: true                                  #boolean, default is false
    uri: dct:identifier                         #uri, can use prefix or fully qualified url
  #column that only has default settings
  first name: {} 
  #column that has some expressions                                  
  last name:                                        
    visible: identifier != null                     #javascript expression, default empth
    validation: if(!/^([a-z]+)$/.test(lastName))'lastName should contain only lowercase letters'                            
                                                    #javascript expression, might refer other columns
                                                    #nb spaces will convert to camelCase
  #example of a reference type
  samples:                                          
    type: ref_array                                 #select, multiselect, radio, checkbox, hierarchy                    
    ref: Samples                                    #table or value set
    refSchema:                                      #can be used for cross schema references
    refLabel: ${id}                                 #defaults to join of key=1 fields
                                                    #only key=x attributes can be references
```

Discussion: we can say names cannot have spaces. Then labels should also be unique so we can upload/download data using labels. But then there is less magic
because the camelCase will not happen???

## Files that define value sets, enums, hierarchies

To define code system. Using 'parent' you can group together. Using 'type=Hierarchy' or 'type=Categorical' you can indicate if code system should be treated as
a hierarchy

```
name: Age groups
description: Subgroups of populations based on age
prefixes:
  ncit: http://purl.obolibrary.org/obo/NCIT_        #also here prefixes can be used
uri: ncit:C20587                                    #can be multiple values
values:                                             #this indicates this is not a table but value set
  Newborn:                                          #string value, should be unique
    label: Neonate                                  #you can relabel terms, should be unique too
    description: |                                  #text description
      An age group comprised of infants during the first month after birth. 
    uri: NCIT_C16731                                #reference to exact term
  Healthy Term Newborn:
    parent: Newborn                                 #to define a hierarchical code system
    uri: ncit:C114930
```

## Files that define a schema

These files are recognized by having 'tables' key in the root

```
name: mySchema                                      #name of the schema (schema name is required)
imports: ../archetypes;otherSchema                  #other schema / table definitions
tables:                                             #ordered map of tables, see above
    Person:                                         #table name is key
        columns:                                    #columns, same definition see above
            Person/*: {}                            #include all columns of imported 'Person'
            Studies/id:                             #include only 'id' column
              label: myid                           #overrides, can only restrict.
              

```

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
    * restrict type such subsetting (type=date instead of datetime, use a subset of ontology)
    * restrict constraingts ()
    * can NOT change type, or remove constraints.
* MIGHT be able to define foreign keys to be of more than one other table
  (as replacement of the inheritance we now have)

## design proposals

* don't use inheritance, instead we use a mixin like 'include' approach to include (parts of) other tables in your current table
    * complications: how to do foreign keys in those cases? previously there were ways to query the differnt supertypes??