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

### Landing page primary cards  text

#### keys
```CATALOGUE_LANDING_COHORTS_TEXT```

```CATALOGUE_LANDING_NETWORKS_TEXT```

```CATALOGUE_LANDING_VARIABLES_TEXT```

#### default
- CATALOGUE_LANDING_COHORTS_TEXT: *A complete overview of all cohorts and biobanks.*
- CATALOGUE_LANDING_NETWORKS_TEXT: *Collaborations of multiple institutions and/or cohorts with a common objective.*
- CATALOGUE_LANDING_VARIABLES_TEXT: *A complete overview of available variables.*

### Landing page info cards (secondary landing card) labels and text

#### keys
```CATALOGUE_LANDING_PARTICIPANTS_LABEL```

```CATALOGUE_LANDING_PARTICIPANTS_TEXT```

```CATALOGUE_LANDING_SAMPLES_LABEL```

```CATALOGUE_LANDING_SAMPLES_TEXT```

```CATALOGUE_LANDING_DESIGN_LABEL```

```CATALOGUE_LANDING_DESIGN_TEXT```


#### default
- CATALOGUE_LANDING_PARTICIPANTS_LABEL: *Participants*
- CATALOGUE_LANDING_PARTICIPANTS_TEXT: *The cumulative number of participants of all datasets combined.*
- CATALOGUE_LANDING_SAMPLES_LABEL: *Samples*
- CATALOGUE_LANDING_SAMPLES_TEXT: *The cumulative number of participants with samples collected of
        all datasets combined.*
- CATALOGUE_LANDING_DESIGN_LABEL: *Longitudinal*
- CATALOGUE_LANDING_DESIGN_TEXT: *Percentage of longitudinal datasets. The remaining datasets are
        cross-sectional.*




## Favicon

A Themed favicon is set by placing a [theme].icon file in the public/img folder. At runtime the [theme] is replaced by the value as set in ```NUXT_PUBLIC_EMX2_THEME``` environment setting. If no theme is set, the default molgenis favicon is show. 

