# Server rendered catalogue

## Schema Settings

### Notice

#### key
```CATALOGUE_NOTICE```

#### description
Notice message to be shown on the main page.

#### default
none, no notice message is shown



### Landing page title

#### key
```CATALOGUE_LANDING_TITLE```

#### description
Main title shown in landing page

#### default
"European Networks Health Data
& Cohort Catalogue."


### Landing page description

#### key
```CATALOGUE_LANDING_DESCRIPTION```

#### description
Description text or subtitle shown on 

#### default
"Browse and manage metadata for data resources, such as cohorts, registries, biobanks, and multi-center collaborations thereof such as networks, common data models and studies."


### Landing page cta (call to action) labels

#### keys
```CATALOGUE_LANDING_COHORTS_CTA```
```CATALOGUE_LANDING_NETWORKS_CTA```
```CATALOGUE_LANDING_VARIABLES_CTA```

#### description
The label shown on landing CTA element for each of the main sections

#### default
"Cohorts", "Networks", "Variables" 








## Favicon

A Themed favicon is set by placing a [theme].icon file in the public/img folder. At runtime the [theme] is replaced by the value as set in ```NUXT_PUBLIC_EMX2_THEME``` environment setting. If no theme is set, the default molgenis favicon is show. 

