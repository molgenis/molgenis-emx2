# Ontology creation logbook

## AgentType

Manually defined subclasses based on [foaf:Agent](http://xmlns.com/foaf/spec/#term_Agent)'s subclasses.

## Availability
Manually extracted from the `planned-availability-skos.rdf` download found [here](https://op.europa.eu/en/web/eu-vocabularies/dataset/-/resource?uri=http://publications.europa.eu/resource/dataset/planned-availability).

## Checksums
Manually extracted from [here](https://spdx.org/rdf/spdx-terms-v2.3/) (seach for "checksum algorithm ").  
Note that v2.3 is used even though DCAT-v3 refers to v2.2 as seen [here](https://www.w3.org/TR/vocab-dcat-3/#Property:distribution_checksum).

## Status
Manually extracted from the `distribution-status-skos.rdf` download found [here](https://op.europa.eu/en/web/eu-vocabularies/dataset/-/resource?uri=http://publications.europa.eu/resource/dataset/distribution-status).

## MediaType
1. Download all `.csv` files from https://www.iana.org/assignments/media-types/media-types.xhtml & store in **single empty** folder.
2. `cd` to folder with the `.csv` files.
3. Run `mkdir output` in this folder.
4. Run the following command: 
    ```bash
   echo "name,definition,codesystem,code,ontologyTermURI" > output/MediaType.csv && \
   awk -F',' 'NR>1 {print $2","$2",IANA,,https://www.iana.org/assignments/media-types/"$2}' *.csv | sort | uniq >> output/MediaType.csv
   ```



## vCard kind

Available definitions retrieved from https://www.w3.org/TR/vcard-rdf/#General_Properties while rest of information was extraced from elsewhere in the document.

## vCard telephone type

The options/URIs were manually extracted from https://www.w3.org/TR/vcard-rdf/ (see for example [this](https://www.w3.org/TR/vcard-rdf/#Code_Sets)).
After which https://www.rfc-editor.org/rfc/rfc6350.html#section-6.4.1 was used to add the definitions.

## vCard type

Available definitions retrieved from https://www.w3.org/TR/vcard-rdf/#Code_Sets while rest of information was extraced from elsewhere in the document.