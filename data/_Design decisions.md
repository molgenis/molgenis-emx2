# Design decisions and guidelines for the RD3 EMX2 Integration

## Guidelines

### Naming system

For all names (tables, columns, etc.), we propose the following format.

1. Table names are title cased. e.g., `Patients`, `Patient analyses`
2. Column names all always lowercased unless doing so breaks the meaning or other standards. E.g., `VCF file`

## Questions

### what standards do we want to interoperate with?

import

- vcf
- phenopacket

output

- dcat/fdp (ejp flavour)
- beacon

### how to model sample -> treatment -> new sample?

proposals:

1. create two tables, biosamples and protocol application. 
2. create one table, biosamples, and record a list of protocols that are applied to this sample.
decision: simplify using protocol(-deviation) to clarify what happened to the sample

### how to model when consent is removed / use is prohibited for specific studies

proposals:

1. create 'off' study for individuals, samples
2. create a list of studies
decision: undecided. Can we have one 'off study' parameter or do we need to record this for each study?

### how to capture protocol specific parameters?

we should have a way to add additional tables to collect these. For example 'sample preparation'

proposals:

1. have Sample preparation (or Xyz protocol) refer to the Biosamples table as a way to add details to that sample. Could have many tables like this.Drawback: no refback possible.
2. have table hierarchy with some abstract 'Protocol parameters' and use subclasses. This would allow a refback to exist to all these tables.
3. use key/value table, so each parameter would become a key, and the parameter value a value. Drawback is loss of forms, validation, cohesion and all that goodness

decision: go for option 2 try it out in demo data. (for portals you might use option 2, but probably we are then better of with a big json field or something)

### how to capture disease specific clinical parameters?

see protocol specific parameters. I.e. we would need to subclass a common 'Individual visit' superclass.
E.g. ithaca disease xyz table would be then a subclass of this table. N.b. we consider other name like 'Individual observation'

### how to model different age representations?

1. categorical age groups
2. using iso period data type

decision: keep both.

### how to deal with cardinality differences between RD3 and standard

- for data we want to import we need to have equivalent cardinality
- for data that we want to export we can have less flexible cardinality

Questions

solve-rd.Individual has;

- 'maternal id' and 'paternal id'
- 'affected' status (we now have this pedigree)
- disease
- recontact incidental
- retracted

Solve-rd.Subject information has

- consangiuity suspected

Samples

- anatomical location other

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

- add validation rules to some of the base types, e.g. 'year' (or even upgrade them to be extended types in emx2 just like we did with email, period, checkbox, radio)