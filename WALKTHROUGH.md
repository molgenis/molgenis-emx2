# Walkthrough demo

This document summarizes a walkthrough demo through MOLGENIS demonstrating its features. We also use this as testing
protocol to verify the latest release.

# Sign in as admin, change password

You sign as admin, using username/password 'admin'. First thing to do is to change the admin password!

- Click on 'sign in'
- Sign in using 'admin' and 'admin'
- Click on 'Hi admin' link
- Change the password, and then 'close' the dialogue
- Sign out and sign in again with your new password
- Try to sign in with faulty user or password, you should get an error message

# Register a new user

You can register new users using the 'sign up' button:

- Make sure you signed out
- Click 'Sign up' and close the sign up form
- Then Sign in as new user
- Then Sign out

# Browse existing databases

We are going to view tables of a database and download contents in various formats.

- Sign in as 'admin' again
- Now you can see all databases in this MOLGENIS
- Click on 'pet store' (you might want to use the search box to quickly find it)
- Click each of the tables to see how that works
- Click download all tables to download in zip, excel. (json-ld and ttl will be empty for pet store because semantic
  annotation is missing).
- Sign out and login again to see results

! feature request: have JSON-LD always return all data, also if not semantically annotated
! known issue, the validations don't work on 'order'

# Upload a data model into a new database

We are going to create a new database and download the contents from pet store.

- Open the Excel file you used download. The 'molgenis' sheet describes the tables (the 'metadata'). The other sheets
  has the contents (the 'data').
- Click 'molgenis' logo to go back to the main screen
- Click on '+' to create a new database 'my store'
- In the dialogue click 'go to upload files' (or close, then click 'my store' and open 'Up/download')
- Click 'browse' and choose to the file you just download and click 'Import Excel'
- On main menu choose 'tables' and then you can see the contents

# Verify more complex data model works

We are going to use the cohort catalogue for this

- Upload cohort catalogue model
  from https://github.com/molgenis/molgenis-emx2/blob/master/data/datacatalogue/molgenis.csv
- Browse 'Databank' and enter a new record, including contributions (this uses complex composite foreign keys, probably
  the most advanced bit of EMX2)
- Download the metadata in excel, csv, json, yaml and verify there is no "mg\_" metadata included
