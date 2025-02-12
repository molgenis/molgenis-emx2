# RDF

The Resource Description Format (RDF) is the W3C standard for web data.
MOLGENIS EMX2 can export data in this format by using the RDF API. This API is located at `<server>/api/rdf` where `<server>` is the location where your MOLGENIS is hosted.
For instance, if your MOLGENIS runs at `https://emx2.test.molgenis.org`, the RDF API is located at `https://emx2.test.molgenis.org/api/rdf`.   

## configuration

The RDF API can be fine-tuned on a schema-based level by adding an advanced setting called `custom_rdf` (some templates might already add this).
If this advanced setting is not present, the RDF API functions based on the default behaviour (f.e. the default list of namespaces).

**The value of the `custom_rdf` advanced setting requires valid Turtle-formatted RDF!**

When this advanced setting is set, it will result in the following:
* The default namespaces will be ignored (except for the schema-specific namespace which is always present).
* Namespaces defined in the `custom_rdf` will used in the RDF API's output.
  * If a `custom_rdf` is set without any namespaces, this means only the schema-specific namespace will be present in the RDF output.
* Any triples in `custom_rdf` will be added to every API call that is part of that schema.

Some notes on multi-schema API calls:
* The triples in `custom_rdf` of all selected schema's will be combined.
* If any of the schemas does not have a `custom_rdf`, the default namespaces will be included as well.
* Conflicts in namespaces will not break the RDF output but might result in unexpected behaviour. Examples include:
  * If 2 different namespaces use the same prefix, only one of them will use that prefix while the other simply returns full IRIs in the API output.
  * If 2 identical namespaces exist with a different prefix, only one of the prefixes will be used for all IRIs belonging to that namespace.


## data retrieval
RDF API retrieve data in different scopes ranging from broad (retrieve everything) to narrow (retrieve one row).
All data is exported as a stream, which means that the response does not include a size estimate.
Listed below are the available options.

### Retrieve everything / multiple schemas
Using `<server>/api/rdf`, all data from this MOLGENIS instance is retrieved and exported as RDF.
Optionally use 'schemas' parameter to filter what schemas to be included. E.g. `<server>/api/rdf?schemas=foo,bar` will only retrieve from schemas 'foo' and 'bar'
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

### Filter rows
The rows from a table within a schema can be filtered based on a column value by adding these as a `key=value` pair to a URL that also contains schema and table name: `<server>/<schema>/api/rdf/<table>?<column-name>=<value>`.
For example: `<server>/pet%20store/api/rdf/Pet?category=cat`

## data formats
Using the content negotiation, RDF can be exported in one of many available formats. For example the following curl command will download the pet store in jsonld:

`curl -H 'Accept: application/ld+json' <server>/pet%20store/api/rdf`

The default format is Turtle, but by passing one of the following mime types in the `Accept:` header you can select a different format.

The recognized mime types are:
- `text/turtle` (Turtle, Terse RDF Triple Language, https://www.w3.org/TR/turtle/)
- `text/n3` (Notation 3, https://www.w3.org/TeamSubmission/n3/)
- `application/n-quads` (N-Quads, https://www.w3.org/TR/n-quads/)
- `application/n-triples` (N-Triples, https://www.w3.org/TR/n-triples/)
- `application/trig` (TriG, https://www.w3.org/TR/trig/)
- `application/rdf+xml` (RDF/XML, https://www.w3.org/TR/rdf-syntax-grammar/)
- `application/ld+json` (JSON-based Serialization for Linked Data, https://www.w3.org/TR/json-ld11/)

### Convenience APIs
As a convience there are APIs to always download in Turtle or JSON-LD:
- `/api/ttl` Downloads always in Turtle
- `/api/jsonld` Downloads always in JSON-LD

One can replace `/api/rdf/` with the above convenience APIs in any of the explained data retrieval API points as
mentioned on this page (such as `<server>/pet%20store/api/jsonld/Pet?category=cat`).