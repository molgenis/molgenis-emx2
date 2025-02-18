# Using semantic feature

You can create useful json-ld or ttl output (see table viewer) by annotating our model.
For this you use the `semantics` field in EMX2.

## Formatting

There are 2 formats allowed for this field:
* an IRI surrounded by `<` and `>` (f.e. `<http://purl.org/dc/elements/1.1/title>`)
* a prefixed name using a defined prefix followed by the relative IRI (f.e. `dcterms:title` if `dcterms` has been defined)

Multiple values can be defined by separating these by a comma (f.e. `dcat:Resource,dcat:Dataset`).

By default, the following prefixed names are available:
<!-- see: https://github.com/molgenis/molgenis-emx2/blob/master/backend/molgenis-emx2-rdf/src/main/java/org/molgenis/emx2/rdf/DefaultNamespace.java -->
<!-- regex-from: ^.*\("([\w\-]+)", "([\d\w:\/\.\-\#]+)".*$ -->
<!-- regex-to: | $1 | $2 | -->

| prefix        | IRI                                            |
|---------------|------------------------------------------------|
| afr           | http://purl.allotrope.org/ontologies/result#   |
| afrl          | http://purl.allotrope.org/ontologies/role#     |
| dc            | http://purl.org/dc/elements/1.1/               |
| dcat          | http://www.w3.org/ns/dcat#                     |
| dcterms       | http://purl.org/dc/terms/                      |
| edam          | http://edamontology.org/                       |
| efo           | http://www.ebi.ac.uk/efo/                      |
| ejp           | https://w3id.org/ejp-rd/vocabulary#            |
| ensembl       | http://ensembl.org/glossary/                   |
| fdp-o         | http://w3id.org/fdp/fdp-o#                     |
| fg            | https://w3id.org/fair-genomes/resource/        |
| foaf          | http://xmlns.com/foaf/0.1/                     |
| healthDCAT-AP | urn:uuid:a7ef52b2-bd43-4294-a80f-3e7299af35e4# |
| hl7           | http://purl.bioontology.org/ontology/HL7/      |
| ldp           | http://www.w3.org/ns/ldp#                      |
| lnc           | http://purl.bioontology.org/ontology/LNC/      |
| mesh          | http://purl.bioontology.org/ontology/MESH/     |
| obo           | http://purl.obolibrary.org/obo/                |
| odrl          | http://www.w3.org/ns/odrl/2/                   |
| ordo          | http://www.orpha.net/ORDO/                     |
| org           | http://www.w3.org/ns/org#                      |
| owl           | http://www.w3.org/2002/07/owl#                 |
| prov          | http://www.w3.org/ns/prov#                     |
| qb            | http://purl.org/linked-data/cube#              |
| rdf           | http://www.w3.org/1999/02/22-rdf-syntax-ns#    |
| rdfs          | http://www.w3.org/2000/01/rdf-schema#          |
| schema        | http://schema.org/                             |
| sio           | http://semanticscience.org/resource/           |
| skos          | http://www.w3.org/2004/02/skos/core#           |
| snomedct      | http://purl.bioontology.org/ontology/SNOMEDCT/ |
| vcard         | http://www.w3.org/2006/vcard/ns#               |
| xsd           | http://www.w3.org/2001/XMLSchema#              |

!> The IRI for healthDCAT-AP is a placeholder as it [currently does not have one defined](https://healthdcat-ap.github.io/#namespaces).

!> The list above can be overridden using a [schema-specific advanced setting](./dev_rdf.md#semantic_prefixes).

## Example

Below, we define that

* table 'patients' contains a record of semantic type 'http://purl.obolibrary.org/obo/NCIT_C16960'
* column 'patients.id' contains an logical 'http://purl.obolibrary.org/obo/NCIT_C83083'
* column 'patient.phenotypes' contains phenotypes 'http://purl.obolibrary.org/obo/NCIT_C16977'
* column 'hpo.termIRI' should be used as the '@id' for 'patients.phenotypes'
  (instead of a local @id, which would be used)

|tableName  |columnName |key |columnType | refTable | semantics                                    |
|-----------|-----------|----|-----------|----------|----------------------------------------------|
|patient    |           |1   |           |          | `<http://purl.obolibrary.org/obo/NCIT_C16960>` |
|patient    |localID    |    |string     |          | `<http://purl.obolibrary.org/obo/NCIT_C83083>` |
|patient    |phenotypes |    |ref_array  |hpo       | `<http://purl.obolibrary.org/obo/NCIT_C16977>` |
|hpo        |termlabel  |1   |string     |          |                                              |
|hpo        |termIRI    |2   |string     |          | id                                           |

### Produces JSON-LD

```json
[
  {
    "@context": {
      "hpo": "http://localhost/patientTest/hpo"
    },
    "@id": "http://localhost/patientTest/hpo",
    "hpo": [
      {
        "termlabel": "Microcephaly",
        "termURI": "http://purl.obolibrary.org/obo/HP_0000252",
        "@id": "http://purl.obolibrary.org/obo/HP_0000252"
      },
      {
        "termlabel": "Congenital microcephaly",
        "termURI": "http://purl.obolibrary.org/obo/HP_0011451",
        "@id": "http://purl.obolibrary.org/obo/HP_0011451"
      }
    ]
  },
  {
    "@context": {
      "patient": "http://localhost/patientTest/patient",
      "localID": "http://purl.obolibrary.org/obo/NCIT_C83083",
      "phenotypes": "http://purl.obolibrary.org/obo/NCIT_C16977"
    },
    "@id": "http://localhost/patientTest/patient",
    "patient": [
      {
        "localID": "no123",
        "phenotypes": [
          "http://purl.obolibrary.org/obo/HP_0000252",
          "http://purl.obolibrary.org/obo/HP_0011451"
        ],
        "@type": "http://purl.obolibrary.org/obo/NCIT_C16960",
        "@id": "http://localhost/patientTest/patient/no123"
      }
    ]
  }
]
```

### Produces TTL

```ttl
<http://localhost/patientTest/hpo> <http://localhost/patientTest/hpo> <http://purl.obolibrary.org/obo/HP_0000252>,
    <http://purl.obolibrary.org/obo/HP_0011451> .

<http://localhost/patientTest/patient> <http://localhost/patientTest/patient> <http://localhost/patientTest/patient/no123> .

<http://localhost/patientTest/patient/no123> a <http://purl.obolibrary.org/obo/NCIT_C16960>;
  <http://purl.obolibrary.org/obo/NCIT_C16977> "http://purl.obolibrary.org/obo/HP_0000252",
    "http://purl.obolibrary.org/obo/HP_0011451";
  <http://purl.obolibrary.org/obo/NCIT_C83083> "no123" .
```
