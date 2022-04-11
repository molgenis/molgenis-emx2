# Using semantic feature

You can create useful json-ld or ttl output (see table viewer) by annotating our model. For this you use the 'semantics'
column in EMX2. Columns without semantics will be omitted.

## Example

Below, we define that

* table 'patients' contains a record of semantic type 'http://purl.obolibrary.org/obo/NCIT_C16960'
* column 'patients.id' contains an logical 'http://purl.obolibrary.org/obo/NCIT_C83083'
* column 'patient.phenotypes' contains phenotypes 'http://purl.obolibrary.org/obo/NCIT_C16977'
* column 'hpo.termIRI' should be used as the '@id' for 'patients.phenotypes'
  (instead of a local @id, which would be used)

|tableName  |columnName |key |columnType | refTable |semantics                                  |
|-----------|-----------|----|-----------|----------|-------------------------------------------|
|patient    |           |1   |           |          |http://purl.obolibrary.org/obo/NCIT_C16960 |
|patient    |localID     |    |string     |          |http://purl.obolibrary.org/obo/NCIT_C83083 |
|patient    |phenotypes |    |ref_array  |hpo       |http://purl.obolibrary.org/obo/NCIT_C16977 |
|hpo        |termlabel  |1   |string     |          |                                           |
|hpo        |termIRI    |2   |string     |          |id                                         |

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
