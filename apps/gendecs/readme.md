# GenDecS prototype
The prototype for Genomic Decision Support. 

## Introduction
This prototype is build inside MOLGENIS EMX2. it contains the
genomics viewer of GenDecS. In the genomics viewer you are able to 
perform a search on patient data using HPO terms. 
The app will check for a given HPO term if there are any matches with 
the variants in the patient data.  
If a match is found a table with the matched variants will be shown.

## Setup

### Server

* Navigate to http://localhost:8080/gendecs/gendecs/#/

### Local

* Create the Patients database. The database can be found in the folder data as Patients.csv
* Create the patient data. The steps can be found [here](https://github.com/JonathanKlimp/GenDecS-tools). 
* Run molgenis emx2. [molgenis guide](https://molgenis.github.io/molgenis-emx2/#/molgenis/use).
* Navigate to http://localhost:8080/[database]/gendecs/#/

## Usage

If you succesfully followed the setup steps you should see a screen 
with the Patient database. 

* Click on a desired patient.
* Click on the button "go to genomics viewer"
* Enter an HPO term and perform a search!