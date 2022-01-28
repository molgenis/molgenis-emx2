# Catalogue model update procedure

A step by step procedure to follow when making changes to the catalogue datamodel. Not all steps are necessary for all model updates.

1. Make changes to the catalogue datamodel in [molgenis.csv](https://github.com/molgenis/molgenis-emx2/blob/master/data/datacatalogue/molgenis.csv). Make sure to increase version number. Test the datamodel changes on a test server.

2. Document the changes from the previous version in [cat-admin.md](https://github.com/molgenis/molgenis-emx2/blob/master/docs/catalogue/cat_admin.md). Also document here how to migrate the data from the previous version to the next. Test the UI changes on the preview server for the PR.

3. Assess whether changes are needed and make necessary changes to UI: variable explorer and catalogue views.

4. Assess whether changes are needed and make necessary changes to [staging area models](https://github.com/molgenis/molgenis-py-catalogue-transform/tree/master/datamodels) for [data-catalogue-staging.molgeniscloud.org](https://data-catalogue-staging.molgeniscloud.org/apps/central/#/). Update the [program to transfer data](https://github.com/molgenis/molgenis-py-catalogue-transform) accordingly.

5. Assess whether changes are needed and make necessary changes to [umcg staging area model](https://raw.githubusercontent.com/molgenis/molgenis-py-cohorts-etl/main/staging-model-umcg.csv) for [catalogue-acc.molgeniscloud.org](https://catalogue-acc.molgeniscloud.org/apps/central/#/). Update the [program to transfer data](https://github.com/molgenis/molgenis-py-cohorts-etl) accordingly.

6. Migrate data/update datamodel on production servers (14-1-2021: data-catalogue, catalogue-acc, conception-acc) according to instructions in [cat-admin.md](https://github.com/molgenis/molgenis-emx2/blob/master/docs/catalogue/cat_admin.md).

7. If necessary migrate data/update staging datamodels on data-catalogue-staging and catalogue-acc.
