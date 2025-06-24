## Todo and open issues

Idea:

- move all columns except primary key from subclass table to rootclass table
- then
  - queries are much simpler and faster (no silly joining needed)
  - metadata of a table subclass tree is easily managed
  - columns can be easily added/removed from/to subclasses
  - still, foreign key constraints can be checked against a subclass table

### Done

- composite foreign key
- fix if drop schema also removes all roles for this schema
- show only schemas in navigator if anonymous or logged in user can view
- ensure columns are retrieved in same order as they were created
- remove 'plus' from central if you don't have permission
- implement settings to point to custom bootstrap css URL
- implement menu setting so we can standardize menu as a default setting
- implement show/hide of columns
- implement order of columns in table view,
- enable user role based menu visibility
- ensure all roles of current user in current schema are returned
- upload files directly into postgresql
- class column in superclass so that can filter on type on export and query
- simplify multiple reference so better understandable how data looks like
- in schema editor, only show local attributes for subclass, not all
- fix graphql link in menu
- change password via ui possible
- enable overlapping composite foreign key for ref_array (trigger difficult!)
- ENSURE PASSWORD IS NOT IN THE LOGS
- download of extended class should include superclass values
- download of superclass should only include superclass records
- enable custom 'format' as way to allow decoration of primitive types such as 'hyperlink', 'email', 'ontology'
- fix alter column in case of composite key (difficult, overlapping columns)
- sanitize column and table identifiers to [\_A-Za-z][_0-9A-Za-z] (we support \_ to replace all illegal characters)
- in table view, don't show subclass rows unless explicitly enabled
- add filter on refSelect in case of overlapping keys
- bookmarkable data explorer state, including back button
- add form elements that are not columns, such as sections (CONSTANT)
- add templates for row and record view to tables, including save as settings
- added theming options by incorporating scss bootstrap service
- changed excel to use streaming APIs to reduce danger of heapspace errors (still all is cached in mem so there are
  limits)
- can upload all subclasses within same Excel/CSV sheet (reduce lookup hell)
- implemented version number in the database to signal incompatible updates
- implement 'draft' so long forms can be saved even when not valid, only pkey is always required
- select for ref/ref*array popup should not show mg* columns
- in forms show ref/ref_array/refback as checkboxes/radio buttons; show in multiple columns;
- make lookup editable
- in catalogue replace 'publications' lookup with a text area for the citation
- breaking change: mg\_\* columns
- first save draft with pkeys only
- Databank.type should only types relevant to conception (but we also want type on this position)
- oicd integration
- as admin can use user manager so I can update passwords for users
- check roundtrip download/update of data and model and settings ('all')
- enable tagging of tables as 'metadata' (or create seperate view for ontologies???)

### todo first

- test ERN data models against EMX2 (to see if we need additional visible/validation options)
- enable autosave; first only show pkey and then all items (like 'new' and 'save')
- add query option to per table csv/excel endpoints to download what you see

cohort catalogue

- in catalogue, change 'release' to 'model'
- in catalogue, seperate 'harmonised' and 'sourcevariables'
- implement order by in table view (not having sorting sucks!)

conception

- Enable column order to be different in subclasses????
- Databank.institution we would like custom refLabel
- Add refLabel as option in schema
- hide 'ontology' tables

### todo second

- enable a 'not null' filter
- for ref/ref_array/refback show only filter options for which you have values, incl counts
- extend catalogue to have all we need to replace lifecycle (i.e. tree filter view)
- hide mg_modifiedBy columns when not editor
- group subclasses in a tree? remodel repeated variables?

### todo next

- enable ref_array to have a 'format' in which it is shown as a sub table
- add favicon and fonts to the theme settings
- enable upload of logo and favicon files instead of linking them
- add audit trail log
- download using filter that is applied in explorer view
- filter option for 'null' and 'not_null'
- add custom roles with per table privileges
- add custom privileges based on policies (row level security)
- change graphql to have pageInfo{first,prev,next,last} pointers returned'
- custom roles, so I can grant priviliges on tables
- create env variable for admin password and add as option to helm chart to ease deploys (now need to change all the
  time)

### later

- postgresql cube index feature for aggregation views
- long running downloads as jobs
- test limits of large data => remove 'offset' and replace with 'after' so large offset doesn't slow down
- enable master/detail edit forms?
- ontology data type
- add properties/settings for each column (to put all extensions)
- investigate migrations (for in place upgrades when metadata tables change)
- per tabel , per rij en per kolom kunnen vragen of men 'edit' permissie heeft
- as user I can can get email when I lost my password
- consider parquet as import/export format
- bug, if I filter on refBack column it fails, must now select reback.other column
- create plugin system for services (todo: isolation? runtime loading?)
- known bug: if I set refBack for refarray to 'null' then ref is not updated!
- user interface for row level security
- more filter option s for array types (now only 'equals')
- improve graphqlError titles and messages
- merge Schema and SchemaMetadata and Table and TableMetadata
- column level permissions
- flattened result in graphql for tables, including group by
  - sorting on nested fields in graphql; showing graphql as flat table
  - csv result field for that flattened result
- Search should work on refBack columns
- group by
- graph mutation next to flat mutation
- decide if we need 'insert' seperate from 'update'
- complete metadata mutations
  - delete column
  - rename column, incl triggers
  - rename table, including triggers
- default limit to 10, maximize on 10.000
- add a check for maximum limit of identifiers, i.e. 63 characters (Excel limit)
- Default values
- Store the descriptions
- Finish the legacy reader
- column/per value validation, tuple/per row validation
- computed values?
- create validation procedure for Schema/Table/Column so we can give complete graphqlError messages and remove model
  checks from from SQL parts

### someday maybe

- throw graphqlError when webservice is called with only csv header and no values
- update is actually upsert (insert ... on conflict update) -> can we make it idempotent 'save' (how to update pkey
  then?)
- taskList api to have long running requests wrapped in a taskList. Should be same as normal api, but then wrapped
- reduce build+test times back to under a minute (LOL)
- decide to store both ends of ref; added value might be order of items and query speed
- cross-schema foreign keys, do we need/want those?
- postgresql queries exposed as readonly tables
