# Guiding principles and features

Below summary of directions that have guided development.

### starting point

* EMX2 simplified metadata format: only one sheet 'molgenis' needed
* Organize data in schemas; each schema functions as permission group and as scope of multi-tenancy if desired
* GraphQL endpoint for each schema, as well as 1 overall
* Uses PostgreSQL for all heavy lifting (incl search, permissions, JSON generation, file storage)
* Can be packaged as one artifact to ease sharing
* Well isolated components (microfrontend using little spa, we envision microservice for server side add-ons)

### dependencies

* Jooq for injection safe database interaction
* Sparkjava for lightweight webservice
* Jackson for json and csv parsing
* POI for Excel parsing
* graphql-java for graphql api
* OpenApi for web service spec (for file based services that don't use graphql)
  To minimize dependencies, no Spring stuff, no Elasticsearch, just the least that can work. Outside scope: file
  service (simple file column type inside postgresql), script service, authentication (asumed all to be other services
  used as dependency)
  Most core ideas where already described
  in https://docs.google.com/document/d/19YEGG8OGgtjCu5WlJKmHbHvosJzw3Mp6e6e7B8EioPY/edit#

### Backend modules

* emx2: interface and base classes
* emx2-sql: implementation into postgresql
* emx2-io: emx2 format, csv import/export of data, legacy import
* emx2-graphql: all for generating the graphql on top of sql
* emx2-semantics: endpoint for linked data serving in json-ld and ttl
* emx2-webapi: ties it all together onto SparkJava embedded web server
* emx2-exampledata: test data models and data, used in various test
* emx2-run: packages all into one fat jar Work in step
* emx2-taskList: toward asynchronous calls for long running transactions/queries

### Feature list (mostly in POC or 'walking skeleton' state)

* simplified EMX '2.0' format
* support for multiple schemas
    - schemas probably should be called 'databases'
    - each project/group can get their own schema
    - each schema will have roles with basic permissions (viewer, editor, creator (for rls), manager, admin)
    - envisioned is that each table will also have these roles, so you can define advanced roles on top
    - row level permission where a 'role' can get edit permission
* permission systems implemented purely using postgresql permission system
    - role based permission system from postresql (view, edit, manage)
    - users are also roles;
    - permissions from molgenis perspective are implemented as default roles on schema, table, row level
    - users can adopt these roles
* extended data definition capabilities
    - simple columnTypes uuid, string, int, decimal, date, datetime, text
    - can create multi-column primary keys and secondary keys ('uniques')
    - can create columns of columnType 'array'
    - can create foreign keys (standard in postgresql)
    - can create arrays of foreign keys (uses triggers)
    - foreign keys can be made to all unique fields, not only primary key (so no mapping between keys needed during
      import)
        - use cascade updates to circument need for meaningless keys
        - checking of foreign keys is defered to end of transaction to ease consistent batch imports
    - can create multi-column foreign keys (discuss if that is useful)
    - many-to-many relationship produce real tables that can be queried/interacted with
* reduced frills and limitations in the metadata
    - there is a metadata schema 'molgenis' with schema, table, column metadata tables
    - no advanced columnTypes; envisioned is that those will be defined as property extensions
    - no UI options are known inside data service; again envisioned to be property extensionos
    - no feature for 'labels', items can only have names for schemas, tables, columns
    - freedom in schema, table and column names; they contain spaces and other charachters beyond a-zA-Z09
* rudimentary import/export for files
    - including molgenis.csv metadata
    - simplified interface to CSV and Excel files (called 'row stores' for now)
* extended query capabilities
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
    - reflection based POJO to table mapping, without heavy metadata / mapping code
    - no magic with lazy loading and stuff
* simple web service
    - somewhat REST like but tuned to our needs
    - make it easy for non REST specialists to use
    - aim to minimize the number of calls