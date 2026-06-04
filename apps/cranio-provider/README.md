# cranio-providers vue application

The `cranio-provider` app is a vue template for creating custom pages for all providers that are associated with ERN CRANIO. This app contains several views for the diagnostic workstreams, breakdowns for center versus ERN summaries, and much more. This template&mdash;along with the UI_DASHBOARD data model&mdash;allow us to create one application that can be rendered for any data provider.

## How this app works

This app is the second ERN CRANIO vue application. The `cranio-public` app is used to display general information about the ERN and to display the public facing dashboard. The cranio-provider app is the _private_ application that is designed for data providers to track and manage submitted data.

### Structure of the app

The app has the following pages. There are several _mini dashboards_ that are specific to the diagnostics workstreams. Within these _mini dashboards_, there are several views that have visualisations specific to the workstream.

```text
<cranio-provider>/
    - Home (landing page and overview)
    - Craniosynostosis
        - All centers overview
            - General overview
            - Surgical overview
        - Your Center overview
            - General overview
            - Surgical overview
    - Cleft lip and palate
        - Overview for the current center
        - Level 1
        - Level 2
    - Facial Dysostosis
        - Level 1
        - Level 2
    - Craniofacial Microsomia
        - Level 1
        - Level 2
    - Genetic hearing loss
        - All centers overview
        - Your center overview
```

### Configuring the app for a provider

On the server, there is a schema for each data provider. This allows us to configure the template to the provider without modifying the frontend code. This is configured by adding the following variables in the advanced settings tab.

| Key                    | Value                    | Description                                             |
|------------------------|--------------------------|---------------------------------------------------------|
| CRANIO_PUBLIC_SCHEMA   | `<public_schema_name>`   | General information (name, location, etc.) and images   |
| CRANIO_PROVIDER_SCHEMA | `<provider_schema_name>` | Summary level statistics on all data providers combined |

When the template loads, it will retrieve the following information and pass it into the template.

1. The CRANIO_*_SCHEMA variables from the advanced settings
2. The current schema name or the ID of the provider
3. Metadata about the current provider

This information is also used to format the API endpoints which eliminates the need to redefined the URLs in each API request.

```js
{
    "current": `/${currentOrganisation?.schemaName}/api/graphql`,
    "public": `/${cranioSchemas?.CRANIO_PUBLIC_SCHEMA}/api/graphql`,
    "providers": `/${cranioSchemas?.CRANIO_PROVIDER_SCHEMA}/api/graphql`,
}
```

If there are any issues with the configuration, an error will be displayed and the app will not be displayed.

## Getting started

### Quick start

Before you start working on the app, it is recommended to create a `.env` file. In it, should be the following variables.

```zsh
# apps/cranio-provider/.env
MOLGENIS_APPS_HOST=...
MOLGENIS_APPS_SCHEMA=...
```

Definitions-

- `MOLGENIS_APPS_HOST`: I recommend using the the beta server as the host. This is the fastest way to get the dev server running. However, I recommend recreating the schemas in your local environment to ensure the changes work with the latest version of EMX2 (see the "Advanced setup" section for more information).
- `MOLGENIS_APPS_SCHEMA`: The schema can be any of the ones created on the beta server. I use `AT1` as it's listed first in the navigation menu, but it is a good idea to use a different schema to confirm that the app works in other situtations.

### Advanced setup

I recommend developing new features and fixing bugs in your local environment. The ERN servers are not always running on the latest version of EMX2 as the tend to be updated when there is a relevant release (e.g., feature, fix, security, etc.). This means downloading necessary schemas and creating them in a clean database. To get started, follow the steps below.

1. __Create the env file__: see the "Quick Start" section for more information
2. __Download schemas__: Go to the beta server and download the following schemas. In each schema, go to the "Up/download page" and export everything as a csv.zip. I also recommend removing all non-dashboard related tables from the schema as this doesn't have any relevance for the vue application.
    - CranioPublic
    - Dashboards
    - AT1 (or another provider schema)
3. __Start a local dev server__:
    - It is a good idea to clean the database: `./gradlew cleandb`
    - Start the server `./gradlew run`
4. __Recreate the schemas__: create the following schemas
    - CranioPublic
        - Create a schema using the `ERN_DASHBOARD` template. Name it `CranioPublic` and do not load the demo data
        - Import the CranioPublic csv zip file
        - Make sure molgenis_settings.csv is loaded
    - Dashboards
        - Create a new schema using the name `Dashboard`. Do not use a template.
        - Import the Dashboards csv zip file
        - Make sure molgenis_settings.csv is loaded
    - AT1
        - Create a new schema using the name `AT1` (or the name of the schema you downloaded). Do not use a template
        - Import the csv zip file
        - Make sure molgenis_settings.csv is loaded

This is all that is needed to get a basic dev instance started. Once everything has imported, you can start the frontend.

```zsh
cd apps/cranio-provider
pnpm dev
```

## Troubleshooting

1. __Error importing components__: If you get an error importing components from `molgenis-viz` or `molgenis-components`, these may need to be built and installed locally. In each folder, run `pnpm build`
2. __Error: Missing the names of the schemas that control the Cranio Provider dashboard...__: If you see this message, then the names of the central schemas are missing from the advanced settings. See the section "Configuring the app for a provider" for more information.
