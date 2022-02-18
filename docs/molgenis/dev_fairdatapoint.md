# FAIR Data Point

FAIR Data Point (FDP, [fairdatapoint.org](https://www.fairdatapoint.org)) is currently being developed for MOLGENIS EMX2.
For MOLGENIS EMX1, the FDP implementation guide can be found [here](https://molgenis.gitbooks.io/molgenis/content/guide-fair.html).

The easiest way to enable FDP in MOLGENIS EMX2 is by uploading a FDP template into an EMX2 database.
A prototype EMX2-FDP template (in XLSX format) is available [here](https://github.com/molgenis/molgenis-emx2/raw/master/docs/resources/FairDataPointTemplate.xlsx).
In addition to the FDP structure, this template includes an example implementation of a FDP developed for 'ERN-FAKE'.

How to import the template and access the FDP endpoints:
* Upload the template into any schema, for instance, a schema called `erns` via `Up/Download`.
* After successful import, the main FDP endpoint then becomes available at `/erns/api/jsonld/fdp_Metadata`.
* This and other endpoints be accessed from the GUI via schema `erns` --> table `fdp_Metadata` --> `download` --> `jsonld`.
* Other tables that are part of the FDP endpoints all start with `fdp_`:
  * fdp_Catalog
  * fdp_Country
  * fdp_Dataset
  * fdp_Distribution
  * fdp_IRI
  * fdp_Language
  * fdp_Metadata
  * fdp_Namespace
  * fdp_Publisher
  * fdp_RightsStatement

Uploading this template, including the ERN-FAKE example, should result in the following `fdp_Metadata` endpoint when deployed at `localhost`.
Obviously, we suggest removing or replacing this example when deploying to any publicly accessible databases.

```
{
  "@context" : {
    "fdp_Metadata" : "http://localhost/erns/fdp_Metadata",
    "title" : "http://purl.org/dc/terms/title",
    "hasVersion" : "http://purl.org/dc/terms/hasVersion",
    "description" : "http://purl.org/dc/terms/description",
    "publisher" : "http://purl.org/dc/terms/publisher",
    "language" : "http://purl.org/dc/terms/language",
    "license" : "http://purl.org/dc/terms/license",
    "alternative" : "http://purl.org/dc/terms/alternative",
    "conformsTo" : "http://purl.org/dc/terms/conformsTo",
    "rights" : "http://purl.org/dc/terms/rights",
    "issued" : [ "fdp:metadataIssued", "r3d:startDate" ],
    "modified" : "fdp:metadataModified",
    "institution" : "http://www.re3data.org/schema/3-0#institution",
    "repositoryContact" : "http://www.re3data.org/schema/3-0#repositoryContact",
    "repositoryType" : "http://www.re3data.org/schema/3-0#repositoryType",
    "api" : "http://www.re3data.org/schema/3-0#api",
    "certificate" : "http://www.re3data.org/schema/3-0#certificate",
    "dataCatalog" : "http://www.re3data.org/schema/3-0#dataCatalog",
    "country" : "http://www.re3data.org/schema/3-0#country",
    "identifier" : "http://rdf.biosemantics.org/ontologies/fdp-o#metadataIdentifier"
  },
  "@id" : "http://localhost/erns/fdp_Metadata",
  "fdp_Metadata" : [ {
    "api" : "FAIR",
    "title" : "ERN-FAKE Patient Registry FAIR Data Point",
    "issued" : "2021-12-10",
    "rights" : "restricted",
    "country" : "NL",
    "license" : "http://rdflicense.appspot.com/rdflicense/UNKNOWN",
    "language" : "eng",
    "mg_draft" : null,
    "modified" : "2021-12-10",
    "publisher" : "molgenis",
    "conformsTo" : "https://www.w3.org/TR/vocab-dcat-2/",
    "hasVersion" : "0.1",
    "identifier" : "fdp_meta",
    "alternative" : "ERN-FAKE Patient Registry FAIR Data Point implemented in MOLGENIS",
    "certificate" : "USERTrust RSA Certification Authority",
    "dataCatalog" : [ "http://localhost/erns/fdp_Catalog/fdp_cat" ],
    "description" : "ERN-FAKE Patient Registry FAIR Data Point implemented in MOLGENIS",
    "institution" : "ERN-FAKE",
    "mg_updatedBy" : "admin",
    "mg_updatedOn" : "2022-02-18T16:53:36.296116",
    "mg_insertedBy" : "admin",
    "mg_insertedOn" : "2022-02-18T16:53:36.296116",
    "repositoryType" : "institutional",
    "repositoryContact" : "Fake University Medical Center",
    "@id" : "http://localhost/erns/fdp_Metadata/fdp_meta"
  } ]
}
```
