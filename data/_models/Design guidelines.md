

We use spaces in column and table names to make them ready for human consumption. This also holds for ontology tables.
The identifier is automatically derived using PascalCase for tables and camelCase for column names.
CSV/Excel import will be forgiving, meaning that casing and spaces are ignored (consequence: columns cannot have same name when  lowercased and spaced removed)

In naming:
- use simple naming that most users that are non specialist also understand
- we prefer to have the related clinical parameters linked in one table, currently called 'clinical' instead of splitting e.g. in diagnosis and other types 
  of events.
- if possible we don't use subclassing, but use more general versatile tables and minimize the number of tables we need for the use cases.

Open questions:
- when to use 'belongs to' or 'part of' or 'has part' or 'links to' ...

For the 'omics' data model we take best practices in names/concepts from:
- FAIR genomes
- RD3
- COSAS
- recon4imd
- To some extend ERNs in particular the CDE
- some of the standards that we know of, e.g. Beacon, MIABIS, FHIR, ... and emerging things like GDI

Later:
- add validation rules to some of the base types, e.g. 'year' (or even upgrade them to be extended types in emx2 just like we did with email, period, 
  checkbox, radio)