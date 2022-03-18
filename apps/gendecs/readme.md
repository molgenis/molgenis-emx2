# GenDecS prototype app
The prototype for Genomic Decision Support.

## Introduction
This prototype is build inside MOLGENIS EMX2

## Usage
With this prototype you are able to search for an HPO term and search for 
a patient in the EMX2 database. 

There is a checkbox present if pressed the children and parents will be searched for the entered HPO term.
This is done using Java jena and SPARQL. SPARQL is used to query 
for the related terms found in [hp.owl](https://hpo.jax.org/app/download/ontology).

When entering a patient id/number a vcfdata.vcf file will automatically be downloaded.
This needs to be placed in data/gendecs.

If a term has been entered and the file has been placed in the directory the button
"search for matches" can be pressed. 
The program will then filter the .vcf file using ClinVar and will match the resulting variants with the selected HPO term. 

The matched variants with ClinVar are written to two files in data/gendecs. One file with
the variant lines from Clinvar and one with the lines from the vcfdata.vcf file.
If a result with the HPO term is found a "match found!" message will appear with the matching genes.

