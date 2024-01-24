# Application profiles

Profiles are a quick way to define new databases by selecting existing tables and columns from a shared data model.
They are defined as simple YAML files found in the [profiles](../../data/_profiles) folder.

## Resources
Profiles are automatically applied to the shared data models present in [models](../../data/_models).
Profile tags from your profile are matched against the tags present in these models.
Whenever selected columns refer to ontologies, these are automatically drawn from the [ontologies](../../data/_ontologies) folder.

It is optional to specify a set of demonstration or specific application data for your profile from the [demodata](../../data/_demodata) folder.
These data are loaded when the option to load demo data is selected when creating a new database from a template in EMX2.
These data may or may not fit the model resulting from your profile, and are simply loaded by 'best effort'.

For advanced users, settings files may be added to your profile to specify menu structure, user rights, etc.
These are loaded from [settings](../../data/_settings).

## Syntax

The most common options for profiles are:

| Attribute   | Ex                                                                                |
|-------------|-----------------------------------------------------------------------------------|
| name        | Short name of this profile.                                                       |
| description | Longer description of this profile.                                               |
| profileTags | Tags that should match tagged tables or columns in the shared data model.         |
| demoData    | Folder with demonstration (i.e. example) data sets of specific application data.  |
| settings    | Folder with settings files such as molgenis_settings.csv or molgenis_members.csv. |

In addition, more specialized options can be used:

| Attribute               | Ex                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ontologiesToFixedSchema | Load any ontologies into a separate schema with the specified name.                                                                                                                                                                                                                                                                                                                                                                                  |
| setViewPermission       | Apply view permissions to specified user or role to the imported schema and ontologies. Includes the schema specified by ontologiesToFixedSchema.                                                                                                                                                                                                                                                                                                    |
| setEditPermission       | Apply editor permissions to specified user or role to the imported schema and ontologies. Includes the schema specified by ontologiesToFixedSchema.                                                                                                                                                                                                                                                                                                  |
| firstCreateSchemasIfMissing | Before creating the schema and ontologies specified by this profile, import one more more other profiles first. Required 3 additional properties to be provided: name, profile and importDemoData. Name specified the name under which the other profile should be imported. Profile points to the profile YAML file. Lastly, importDemoData is either true or false and specified whether the demo data for this profile should be imported or not. |

## Special tagging behaviour

- Tagging a table but none of its columns applies tags all of its columns 

When a table header (i.e. a data model row without a columnType) is tagged with a particular tag,  but none of its columns have that particular tag, all of the columns are automatically tagged with that particular tag.
This makes it easy to quickly select complete tables when using tags to compose a new profile.
However, when even one column does carry a tag, only that column will be selected for that table and all others will be ignored.

- Parent tables are automatically tagged when using inheritance

When using inheritance, parent tables (and their parents, and so on) are automatically tagged when a child table is tagged.
This removed the need to tag these tables and thereby automatically tagging all of its columns
In other words, these parent tables are automatically included as column-less tables.
However, keep in mind that primary keys present in parent tables may still need to be tagged explicitly.

## Complete example
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
