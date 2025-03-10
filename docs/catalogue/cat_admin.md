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
