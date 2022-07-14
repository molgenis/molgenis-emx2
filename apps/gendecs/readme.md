# GenDecS prototype

## About
This prototype is build inside MOLGENIS-EMX2. it contains the genomics viewer of GenDecS.  
In the genomics viewer you are able to 
perform a search on patient data using HPO terms. 
The app will check for a given HPO term if there are any associated variants in the patient data.    
If a match is found a table with the matched variants will be shown.

## Setup

* Follow the steps in apps/gendecs/HowToRun.md.

Make sure apps/dev-proxy.config.js has the following variables:
```
const HOST = process.env.MOLGENIS_APPS_HOST || "http://localhost:8080";
const SCHEMA = process.env.MOLGENIS_APPS_SCHEMA || "gendecs";
```

## Usage

If you successfully followed the setup steps you should see a screen 
with the Patient database. 

* Click on a patient.
* Click on the button "go to genomics viewer"
* Enter a HPO term and perform a search!

## Contact

Jonathan Klimp  
email: j.klimp@st.hanze.nl
