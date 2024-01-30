# Application profiles

Profiles are a quick way to define new databases by selecting existing tables and columns from a shared data model.
They are defined as simple YAML files found in the [profiles](../../data/_profiles) folder.

Profiles currently make use of a number of available resources that are packaged with EMX2.
These resources include data models, ontologies, demodata and settings.
For more details on how to use these resources, see the Resources section below.

Profiles use specific YAML field names as its options.
For more details on how to use these options, see the Options section below.

## Resources

Profiles are automatically applied to the shared data models present in [models](../../data/_models).
Profile tags from your profile are matched against the tags present in these models.
Whenever selected columns refer to ontologies, these are automatically drawn from the [ontologies](../../data/_ontologies) folder.

It is optional to specify a set of demonstration or specific application data for your profile from the [demodata](../../data/_demodata) folder.
These data are loaded when the option to load demo data is selected when creating a new database from a template in EMX2.
These data may or may not fit the model resulting from your profile, and are simply loaded by 'best effort'.

For advanced users, settings files may be added to your profile to specify menu structure, user rights, and more.
These are loaded from [settings](../../data/_settings).

## Options

The most common options for profiles are:

| Option      | Description                                                                       |
|-------------|-----------------------------------------------------------------------------------|
| name        | Short name of this profile.                                                       |
| description | Longer description of this profile.                                               |
| profileTags | Tags that should match tagged tables or columns in the shared data model.         |
| demoData    | Folder with demonstration (i.e. example) data sets of specific application data.  |
| settings    | Folder with settings files such as molgenis_settings.csv or molgenis_members.csv. |

In addition, more specialized options can be used:

| Option                      | Description                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ontologiesToFixedSchema     | Load any ontologies into a separate schema with the specified name.                                                                                                                                                                                                                                                                                                                                                                                  |
| setViewPermission           | Apply view permissions to specified user or role to the imported schema and ontologies. Includes the schema specified by ontologiesToFixedSchema.                                                                                                                                                                                                                                                                                                    |
| setEditPermission           | Apply editor permissions to specified user or role to the imported schema and ontologies. Includes the schema specified by ontologiesToFixedSchema.                                                                                                                                                                                                                                                                                                  |
| firstCreateSchemasIfMissing | Before creating the schema and ontologies specified by this profile, import one more more other profiles first. Required 3 additional properties to be provided: name, profile and importDemoData. Name specified the name under which the other profile should be imported. Profile points to the profile YAML file. Lastly, importDemoData is either true or false and specified whether the demo data for this profile should be imported or not. |

## Complete example

This example profile contains all currently supported options:

```yaml
---
name: SharedStaging
description: "This schema contains communal tables that can be altered by all users."
profileTags: SharedStaging
demoData: _demodata/applications/datacatalogue_sharedstaging
settings: _settings/datacatalogue_sharedstaging

# special options
ontologiesToFixedSchema: CatalogueOntologies
setViewPermission: anonymous
setEditPermission: user
firstCreateSchemasIfMissing:
  - name: catalogue
    profile: _profiles/DataCatalogue.yaml
    importDemoData: true
  - name: SharedStaging
    profile: _profiles/SharedStaging.yaml
    importDemoData: true
```
