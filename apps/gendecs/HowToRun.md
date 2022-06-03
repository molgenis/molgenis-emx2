# Do the following to run GenDecS:

## Files needed to run GenDecS:
* ClinVar: clinvar_20220528.vcf.gz from: https://ftp.ncbi.nlm.nih.gov/pub/clinvar/vcf_GRCh37/ 
* HPO ontology: https://github.com/obophenotype/human-phenotype-ontology/blob/v2022-04-14/hp.owl or from: https://hpo.jax.org/app/download/ontology 
* Genes_to_phenotype.txt: https://github.com/obophenotype/human-phenotype-ontology/releases/tag/v2022-04-14 or from: https://hpo.jax.org/app/download/annotation 

## Creation of database
1. clone emx2 (https://github.com/JonathanKlimp/molgenis-emx2)
2. create directory gendecs in the folder data
3. place the HPO ontology and genes_to_phenotype.txt in /data/gendecs/
3. follow the installation steps of EMX2 (https://molgenis.github.io/molgenis-emx2/#/molgenis/run_java / https://molgenis.github.io/molgenis-emx2/#/molgenis/dev_quickstart)
4. navigate to http://localhost:8080
5. login to EMX2 with username:admin password:admin
6. go to the Databases tab or navigate to http://localhost:8080/apps/central/#/
7. create a database called "gendecs"
8. Inside the database gendecs import the Patients.csv table found here: https://github.com/JonathanKlimp/molgenis-emx2/tree/master/apps/gendecs/data

## Upload data to the database
9. Download each file from: https://github.com/JonathanKlimp/GenDecS-tools/tree/main/gendecsTestData
10. Download each file from: https://github.com/JonathanKlimp/GenDecS-tools/tree/main/builds
11. place the data in a seperate folder.
12. open the terminal in this folder.
13. execute the following command:
    for FILE in *; do sh pathTo/GenDecS_filter_and_upload_pipeline.sh -f $FILE -o [output dir] -c [path to clinvar] -g [path to genes_to_phenotype.txt]; done
14. When finished all data should be uploaded to EMX2 and a new table vcf_variants should exist.
    If you can't see this table try to log out and log back in.

## Final steps
15. navigate to: http://localhost:8080/gendecs/tables/#/Patients
16. Click on a patient and try GenDecS!