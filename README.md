[![Build Status](https://travis-ci.org/mswertz/molgenis-emx2.svg?branch=master)](https://travis-ci.org/mswertz/molgenis-emx2)
[![Quality Status](https://sonarcloud.io/api/project_badges/measure?project=mswertz_molgenis-emx2&metric=alert_status)](https://sonarcloud.io/dashboard?id=mswertz_molgenis-emx2)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=mswertz_molgenis-emx2&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=mswertz_molgenis-emx2)

# molgenis-emx2
POC for EMX2.
EMX2 is NOT a ORM. But more like a table gateway.
I.e. foreign keys are not lazy loaded and object trees NOT persisted
Instead EMX allows you to precisely manipulate the database backend.

## Feature list
*  support for multiple schemas
*  fluent API for query and ddl
*  can create query accross multiple tables via column expansion
*  composite keys
*  same metadata model everywhere
*  simple pojo to database mapping
*  thin layer on sql
*  array data types
*  arrays can be foreign keys (as one-way many-to-many)
*  mref tables are real tables
*  Simplify reference to default key
*  composite foreign keys
*  lightweight web service
*  openapi support
*  Improve relationships config, with defaults to primary key etc

## TODO urgent

*  test roundtrip of metadata loading via MetadataUtils
*  including delete/drop behavior 

# TODO later
*  refactor query to have 'WhereList' for each 'OR' clause and introduce a 'Filter' concept as items in the Where
*  refactor Table / TableMetadata as seperate class hierarchies and using decorator pattern
*  decide if we keep molgenisid column (arg for: never changes) or let users define the key
*  TEST QUERY A LOT!!!!
*  query join of REF(now you must follow foreign keys)
*  import/export
*  see if WHERE can extend QUERY for better fluent API
*  table or join inheritance
*  separate sql from ui data types
*  ADD LOCAL TRANSACTIONS FOR MULTI_COMMAND OPERATIONS
*  enable search on joined tables in query
*  Default values
*  legacy reader
*  implement unqiue and primary key annotations for class->table mapping

## Discussion

* how to best project into json schema. Confusing because ideally one schema would be a json schema so you can treat molgenis as big json store.
