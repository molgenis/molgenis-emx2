# System administrator

## Server settings

### Demo dataset

#### key

`MOLGENIS_INCLUDE_CATALOGUE_DEMO`

#### description

Boolean defining whether to load the demo catalogue on server startup

#### default

`false`

## Schema settings

### Notification

#### key

`CATALOGUE_BANNER_HTML`

#### description

String containing html to be rendered in page banner

#### default

None, no notification message is shown

### Landing page title

#### key

`CATALOGUE_LANDING_TITLE`

#### description

Main title shown on landing page

#### default

"European Networks Health Data
& Cohort Catalogue."

### Landing page description

#### key

`CATALOGUE_LANDING_DESCRIPTION`

#### description

Description text or subtitle shown on landing page

#### default

"Browse and manage metadata for data resources, such as cohorts, registries, biobanks,
and multi-center collaborations thereof such as networks, common data models and studies."

### Landing page call-to-action (CTA) labels

#### keys

`CATALOGUE_LANDING_COHORTS_CTA`

`CATALOGUE_LANDING_NETWORKS_CTA`

`CATALOGUE_LANDING_VARIABLES_CTA`

#### description

The label shown on landing CTA element for each of the main sections

#### default

"Cohorts", "Networks", "Variables"

### Landing page call-to-action (CTA) descriptions

#### keys

`CATALOGUE_LANDING_COHORTS_TEXT`

`CATALOGUE_LANDING_NETWORKS_TEXT`

`CATALOGUE_LANDING_VARIABLES_TEXT`

#### description

The descriptive text shown underneath each CTA element

#### default

- `CATALOGUE_LANDING_COHORTS_TEXT`: "A complete overview of all cohorts and biobanks."
- `CATALOGUE_LANDING_NETWORKS_TEXT`: "Collaborations of multiple institutions and/or cohorts with a common objective."
- `CATALOGUE_LANDING_VARIABLES_TEXT`: "A complete overview of available variables."

### Landing page information cards

#### keys

`CATALOGUE_LANDING_PARTICIPANTS_LABEL`

`CATALOGUE_LANDING_PARTICIPANTS_TEXT`

`CATALOGUE_LANDING_SAMPLES_LABEL`

`CATALOGUE_LANDING_SAMPLES_TEXT`

`CATALOGUE_LANDING_DESIGN_LABEL`

`CATALOGUE_LANDING_DESIGN_TEXT`

`CATALOGUE_LANDING_SUBCOHORTS_LABEL`

`CATALOGUE_LANDING_SUBCOHORTS_TEXT`

#### description

Labels and descriptive texts for the information cards on the landing page

#### default

- `CATALOGUE_LANDING_PARTICIPANTS_LABEL`: "Participants"
- `CATALOGUE_LANDING_PARTICIPANTS_TEXT`: "The cumulative number of participants of all datasets combined."
- `CATALOGUE_LANDING_SAMPLES_LABEL`: "Samples"
- `CATALOGUE_LANDING_SAMPLES_TEXT`: "The cumulative number of participants with samples collected of all datasets combined."
- `CATALOGUE_LANDING_DESIGN_LABEL`: "Longitudinal"
- `CATALOGUE_LANDING_DESIGN_TEXT`: "Percentage of longitudinal datasets. The remaining datasets are cross-sectional"
- `CATALOGUE_LANDING_SUBCOHORTS_LABEL`: "Subcohorts"
- `CATALOGUE_LANDING_SUBCOHORTS_TEXT`: "The total number of subcohorts included"

## Favicon

A themed favicon is set by placing a [theme].ico file in the public/img folder.
At runtime the [theme] is replaced by the value as set in the `NUXT_PUBLIC_EMX2_THEME` environment setting.
If no theme is set, the default MOLGENIS favicon is shown.

## Analytics

Analytics can be enabled by setting the following environment variables:

`NUXT_PUBLIC_ANALYTICS_KEY`: The analytics measurement id.

`NUXT_PUBLIC_ANALYTICS_PROVIDER`: The analytics provider. Either `siteimprove` for [Siteimprove](https://www.siteimprove.com/)
or `google-analytics` for [Google Analytics](https://marketingplatform.google.com/about/analytics/).
Defaults to `siteimprove`.

## Data model changelog

Below is a list of versions of the data model with a description of changes made for each version.
Note that the list only goes back to the most recent breaking change, i.e. the most recent major version update.

| Version | Changes                                                                                         |
|---------|-------------------------------------------------------------------------------------------------|
| 5.0     | Breaking change from version 4.x due to renaming _Collections_ to _Resources_.                  |
| 5.1     | Made 'type' a required property for _Resources_.                                                |
| 5.2     | Introduced table _Internal identifiers_. Added exclusion criteria to _Resources_.               |
| 5.3     | Added clarification on use of keywords.                                                         |
| 5.4     | Added UMCU and INTEGRATE cohorts profiles.                                                      |
| 5.4.x   | Added data items for valid DCAT HRICore 1.0: contact point, publisher, theme, creator, issued, modified.|
| 5.4.x   | Added tables for Fair Data Point: Container, Endpoint, Agent.                                   | 
| 5.5     | Fixed reflinks, typos and added demo data to fit DCAT HRICore 1.0.                              |
| 5.6     | Removed computed value for Catalogues.description. Added missing demo data descriptions.        |
| 6.0     | Breaking change. We merged 'Catalogues' into 'Resources'. To solve download from old, import in new and then in Resourcs.types set 'catalogue' to those records previously linked in Catalogues'	    |
| 6.0.1   | Add Resources.publications and Resources.information to INTEGRATE profile. To update reload updated molgenis.csv to INTEGRATE cohorts staging areas.    |
| 6.0.2   | Add Resources.catalogue types to NetworksStaging profile. To update reload updated molgenis.csv to Network staging areas    |