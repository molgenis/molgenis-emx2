[![Build Status](https://travis-ci.org/mswertz/molgenis-emx2.svg?branch=master)](https://travis-ci.org/mswertz/molgenis-emx2)
[![Quality Status](https://sonarcloud.io/api/project_badges/measure?project=mswertz_molgenis-emx2&metric=alert_status)](https://sonarcloud.io/dashboard?id=mswertz_molgenis-emx2)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=mswertz_molgenis-emx2&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=mswertz_molgenis-emx2)

# molgenis-emx2 preview
This is a reference implementation of MOLGENIS/EMX2 data service. Status: preview.

## How to run
Fastest is to build and then user docker:

```console
mvn package -Dmaven.test.skip=true
docker-compose up
```

Then you want to browse to http//localhost:8080
Most fun you have with graphql playground.
First thing to do: login as admin (passwords are not yet needed)

```
mutation{
login(username:"admin"){message}
}
```

Alternatively:
* install postgres 11
* create database 'molgenis' with superadmin user/pass molgenis
* run molgenis-emx2-webservice/src/org.molgenis.emx2.RunMolgenisEmx2

## Features
* EMX2 simplified metadata format
* Support for multiple schemas; each schema functions as permission group
* GraphQL endpoint
* Uses PostgreSQL for all heavy lifting (incl permissions, JSON generation)

## how to run
*  need install of postresql 11 with superadmin molgenis/molgenis
*  mvn test is most interesting to see if and how all works
*  emx2-webservice/test RunWebApi is most interesting to play with
*  emx2-io/test/resources/test1.txt gives idea on EMX2 format

## minimal dependencies
* PostgresQL for all heavy lifting (transactions, permissions, json generation)
* Jooq for safe database interaction 
* Sparkjava for lightweigh webservice
* Jackson for json and csv parsing
* POI for Excel parsing
* OpenApi for web service spec
* graphql-java for graphql api
Minimizes dependencies, no Spring stuff, no Elasticsearch, just the least that can work.
Outside scope: file service, script service, authentication (asumed all to be other services on top)
Most core ideas where already described in https://docs.google.com/document/d/19YEGG8OGgtjCu5WlJKmHbHvosJzw3Mp6e6e7B8EioPY/edit#

## modules
*  emx2: interface and base classes, concept only
*  emx2-jooq: implementation into postgresql
*  emx2-io: emx2 format, csv import/export of data, legacy import
*  emx2-webservice: web API on top of jooq + io.
*  emx2-exampledata: test data models and data, used in various test

## Feature list (mostly in POC or 'walking skeleton' state)
*  simplified EMX '2.0' format 
    - only one 'molgenis.csv' metadata file (instead of now multiple for package, entity, attribute)
    - reducing the width of spreadsheet to only 5 columns: schema, table, column, properties, description
    - the 'properties' column is where all the constraints happen
        - using simple tags as 'int'
        - or parameterised properties as 'ref(otherTable,aColumn)'
    - properties can be defined for schema, table, or column
    - format is designed to be extensible with properties not known in backend
    - rudimentary convertor from EMX1 to EMX2
*  support for multiple schemas
    - schemas probably should be called 'groups'
    - each project/group can get their own schema 
    - each schema will have roles with basic permissions (viewer, editor, creator (for rls), manager, admin)
    - envisioned is that each table will also have these roles, so you can define advanced roles on top
    - row level permission where a 'role' can get edit permission
*  permission systems implemented purely using postgresql permission system
    - role based permission system from postresql (view, edit, manage)
    - users are also roles; 
    - permissions from molgenis perspective are implemented as default roles on schema, table, row level
    - users can adopt these roles
*  extended data definition capabilities
    - simple columnTypes uuid, string, int, decimal, date, datetime, text
    - can create multi-column primary keys and secondary keys ('uniques')
    - can create columns of columnType 'array'
    - can create foreign keys (standard in postgresql)
    - can create arrays of foreign keys (uses triggers)
    - foreign keys can be made to all unique fields, not only primary key (so no mapping between keys needed during import)
        - use cascade updates to circument need for meaningless keys
        - checking of foreign keys is defered to end of transaction to ease consistent batch imports
    - can create multi-column foreign keys (discuss if that is useful)
    - many-to-many relationship produce real tables that can be queried/interacted with
*  reduced frills and limitations in the metadata
    - there is a metadata schema 'molgenis' with schema, table, column metadata tables
    - no advanced columnTypes; envisioned is that those will be defined as property extensions
    - no UI options are known inside data service; again envisioned to be property extensionos
    - no feature for 'labels', items can only have names for schemas, tables, columns
    - freedom in schema, table and column names; they contain spaces and other charachters beyond a-zA-Z09
*  rudimentary import/export for files
    - including molgenis.csv metadata
    - simplified interface to CSV and Excel files (called 'row stores' for now)
*  extended query capabilities
    - can query in joins accross tables by following foreign key references
    - query model similar to trac i.e.
        - each condition can have multiple values, i.e. a or b or c
        - multiple conditions are assumed to be combined with 'and' into one clause
        - multiple clauses can be provided assuming 'or' between them
* basic search capability using Postgresql full text search capability
    - will be very interesting to see how this compares in cost/features to our needs, and elastic.
    - see https://www.postgresql.org/docs/11/textsearch.html
* programmer friendly API (or at least, that is what I think)
    - Rows class enables columnType safe code, with magic columnType conversions
    - assume always sets / arrays / batches
    - reflection based POJO to table mapping, without  heavy metadata / mapping code 
    - no magic with lazy loading and stuff
* simple web service
    - somewhat REST like but tuned to our needs
    - make it easy for non REST specialists to use
    - aim to minimize the number of calls

## Todo and open issues
*  metadata change listener so we know when to clear api & metadata caches
*  group by
*  flattened result in graphql for tables, including group by
    *  sorting on nested fields in graphql; showing graphql as flat table
    *  csv result field for that flattened result
*  graph mutation next to flat mutation
*  decide if we need 'insert' seperate from 'update'
*  test and fix the openapi so all the docs just work
*  'save' for graphql allowing save of whole graphs
*  Search should work on refback columns; lazy could be to keep copy in sync.
*  partial/patch updates, i.e. null columns versus missing (to be ingored) columns
*  delete column
*  default limit to 10
*  add a check for maximum limit of identifiers, i.e. 63 characters (Excel limit)
*  throw error when webservice is called with only csv header and no values
*  update is actually upsert (insert ... on conflict update) -> can we make it idempotent 'save' (how to update pkey then?)
*  job api to have long running requests wrapped in a job. Should be same as normal api, but then wrapped
*  Default values
*  Decide on free table/column names vs sanitized graphql
*  Store the descriptions
*  Finish the legacy reader
*  Decide to remove multi-column pkey
*  multi-column ref is not yet reloadable (huge challenging bug, not sure whether we should support that)
*  sanitize column and table identifiers to [_A-Za-z][_0-9A-Za-z] (we support _ to replace all illegal characters)

## helpfull notes

In Excel you can use following macro to export all into CSV files.

``
Private Sub cmdSave()

Dim WS As Excel.Worksheet
Dim CurrentWorkbook As String
Dim CurrentFormat As Long

CurrentWorkbook = ThisWorkbook.FullName
CurrentFormat = ThisWorkbook.FileFormat

For Each WS In Application.ActiveWorkbook.Worksheets
    WS.SaveAs WS.Name & ".csv", xlCSVUTF8
Next

End Sub
``



