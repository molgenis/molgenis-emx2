# GDI Local Portal

This vue application is designed to the work the FAIR Data hub schema and any datasets imported into the data model. There are two search pages.

1. Dataset Search: find collections of records using the GraphQL API
2. Individuals Search: find individuals (i.e., patients, participants, etc.) in a database by gender and genes

In order to get the search pages to work, the minimun data requirements are -

1. A dataset that populates these tables: Dataset, Distribution, Catalog, and Files tables
2. A dataset that populates the Individuals table. This must include an identifier, sex, and genes.

## Getting started

To use this vue app, follow these steps.

### Create a new database

1. Sign in to your MOLGENIS EMX2 instance
2. Click the create a database button
3. In the new menu, give it a unique name and select the `FAIR_DATA_HUB` template. Alternatively, you can use the `GDI` template.
4. Select "Include example data" or leave it as false if you have your own data. (You can always delete the example data later.)
5. Save the form and wait for the database to create. (This should take a few seconds.)

### Create a link with REMS

If you would like to use the request service (REMS), you must set the REMS host in the settings tab. If so -

1. Click your newly created schema
2. Click the "Settings" link in the menu bar, and the "Advanced Settings" tab.
3. Click the "add new setting" button, and enter the following information -
    - Key: `REMS_URL`
    - Setting Value: `https://<your-rems-host>/`
4. Save the entry

### Add the vue app as the home page

To make the navigation a bit clearer, you can create a new menu link.

1. Go to your newly created schema
2. Click the "Settings" link in the menu bar, and then the "Menu" tab.
3. Click the "add a new link" button and enter the following information
    - Label: `Home` or any other name
    - Href: `./gportal/` (required)
    - Role: select the "Viewer" option
