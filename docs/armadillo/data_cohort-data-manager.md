# Data manager of a cohort or data source

## Armadillo

Upload harmonised data into a local Armadillo/Opal instance in order to make it available for DataSHIELD to run analyses. Note that MOLGENIS Data Catalogue does not communicate or have access to any data stored in Armadillo/Opal.

### Request access (Armadillo)

The Armadillo works with a central authentication service. This means to work with the Armadillo you need to have an account on the central authentication service. To acquire an account please follow instructions on [this video](https://youtu.be/Gj0uANX8nIw).

### Initial upload

There are two phases to uploading data to the Armadillo. The initial upload transforms your source data to the right format for analysis. Besides this you can perform some data manipulation on the initially uploaded data.

The initial upload can be done with the [dsUpload](https://lifecycle-project.github.io/ds-upload).

### Create subsets

To manipulate the data after the initial upload you can use the [MolgenisArmadillo](https://molgenis.github.io/molgenis-r-armadillo) client.
You can use the [MolgenisArmadillo](https://molgenis.github.io/molgenis-r-armadillo) to create data subsets. Check the [documentation](https://molgenis.github.io/molgenis-r-armadillo/articles/creating_data_subsets.html) to create subsets.

### Assign permissions

After the correct authentication and authorisation steps have been set up, researchers will be able to analyse the cohort’s data via DataSHIELD. We use the authentication service to give people permission to analyse the data. There are several steps you need to perform to give people access:

- create a role
- register a user
- give a user a role

We assume you have already created the necessary data sets for the researcher in question. After that you can navigate to the authentication service of the Armadillo. This [manual](https://molgenis.github.io/molgenis-js-auth) describes how to assign permissions to researchers.

### Quality control

There are two levels of quality control, central and local. You can perform local quality control yourself. This is done on the dataset you harmonised for the project. Usually the local quality control scripts are developed and distributed in the harmonisation manual.

For central quality control we use the [dsUpload](https://lifecycle-project.github.io/ds-upload/articles/qualityControl.html) package. We are now developing quality control measures for the different variables. Continues, repeated etc. This is not finished yet. We will let you know when this is useable.

### Deploy an instance

To get the Armadillo installed at your institute you need to contact your IT-department and send the [installation manual](https://galaxy.ansible.com/molgenis/armadillo).

The system administrator needs to have specific information to set up the Armadillo. Each Armadillo is bound to a central authentication server. There needs to be an entry in this central authentication server for the Armadillo. You can email [*molgenis-support*](mailto:molgenis-support@umcg.nl) to get the specific information that applies to your Armadillo instance.
