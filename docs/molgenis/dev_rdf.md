# RDF

The Resource Description Format (RDF) is the W3C standard for web data.
MOLGENIS EMX2 can export data in this format by using the RDF API. This API is located at `<server>/api/rdf` where `<server>` is the location where your MOLGENIS is hosted.
For instance, if your MOLGENIS runs at `https://emx2.test.molgenis.org`, the RDF API is located at `https://emx2.test.molgenis.org/api/rdf`.   

## RDF data retrieval
RDF API retrieve data in different scopes ranging from broad (retrieve everything) to narrow (retrieve one row).
All data is exported as a stream, which means that the response does not include a size estimate.
Listed below are the available options.

### Retrieve everything
Using `<server>/api/rdf`, all data from this MOLGENIS instance is retrieved and exported as RDF.
Of course, this is limited to data to which the currently logged-in user (or anonymous user) has access to.

### Retrieve one schema
By including a database schema name in the URL, data from one particular schema is retrieved and exported as RDF.
The schema name is added between the server location and RDF API location: `<server>/<schema>/api/rdf`.
For example: `<server>/pet%20store/api/rdf`.

### Retrieve one table
One particular table from a schema can be retrieved by adding a table name to a URL that also contains schema name: `<server>/<schema>/api/rdf/<table>`.
For example: `<server>/pet%20store/api/rdf/Pet`

### Retrieve one column
One particular column from a table within a schema can be retrieved by adding a column name to a URL that also contains schema and table name: `<server>/<schema>/api/rdf/<table>/column/<column-name>`.
For example: `<server>/pet%20store/api/rdf/Pet/column/name`

### Retrieve one row
One particular row from a table within a schema can be retrieved by adding a row identifier to a URL that also contains schema and table name: `<server>/<schema>/api/rdf/<table>/<row-id>`.
For example: `<server>/pet%20store/api/rdf/Pet/spike`

## RDF data formats
Using the `format` parameter, RDF can be exported in one of many available formats. The `format` parameter is passed through the URL, for instance `<server>/pet%20store/api/rdf?format=xml`.
 If this parameter is not specified, the default format (`ttl`) will be automatically chosen. So, for instance, `<server>/pet%20store/api/rdf/Pet/spike` returns the exact same result as `<server>/pet%20store/api/rdf/Pet/spike?format=ttl`.

The available formats are:
- `ttl` (Turtle, Terse RDF Triple Language, https://www.w3.org/TR/turtle/)
- `n3` (Notation 3, https://www.w3.org/TeamSubmission/n3/)
- `nquads` (N-Quads, https://www.w3.org/TR/n-quads/)
- `ntriples` (N-Triples, https://www.w3.org/TR/n-triples/)
- `trig` (TriG, https://www.w3.org/TR/trig/)
- `xml` (RDF/XML, https://www.w3.org/TR/rdf-syntax-grammar/)
- `jsonld` (JSON-based Serialization for Linked Data, https://www.w3.org/TR/json-ld11/)
