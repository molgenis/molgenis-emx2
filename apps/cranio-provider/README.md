# Dashboard template for ERN Cranio providers

The `cranio-provider` app is a template for creating custom pages for all providers that are associated with ERN CRANIO. This app contains several views for the diagnostic workstreams, breakdowns for center versus ERN summaries, and much more. As a result, the application is structured using nested router views.

The structure of the app is:

```text
cranio-provider
    - Center landing page and overview
    - Craniosynostosis
        - All centers overview
            - General overview
            - Surgical overview
        - Your Center overview
            - General overview
            - Surgical overview
    - Cleft lip and palate
        - Overview for the current center
    - Genetic hearing loss
        - All centers overview
        - Your center overview
```

The app works by setting two settings. This allows the vue applications to be independent and to avoid hardcoding any schema information except in the database itself.

| Key                    | Value                    |
|------------------------|--------------------------|
| CRANIO_PUBLIC_SCHEMA   | `<public_schema_name>`   |
| CRANIO_PROVIDER_SCHEMA | `<provider_schema_name>` |

When the template loads, it will use the PUBLIC schema to find the relevant data about the current schema (e.g., AT1) and retrieve content (e.g., images, titles, etc.). The provider schema is where all ERN-wide aggregations are stored. This allows us to produce these once and store them in a central location. Requests are made to retrieve this information and merge it with data about the current center.

## Quick start

Before you start working on the app, it is recommended to create a `.env` file. In it, should be the following variables.

```zsh
# apps/cranio-provider/.env
MOLGENIS_APPS_HOST=...
MOLGENIS_APPS_SCHEMA=...
```

Definitions-

- `MOLGENIS_APPS_HOST`: I recommend using the the beta server as the host (see the server list for this URL). This is the fastest way to get the dev server running; however, it is better to recreate the schemas in your local environment to ensure the changes work with the latest version of EMX2.
- `MOLGENIS_APPS_SCHEMA`: The schema can be any of the ones created on the beta server. I use `AT1` as it's listed first in the navigation menu, but it is a good idea to use a different schema to confirm that the app works in other situtations.

## Advanced setup

It is recommended to develop features and fix bugs using a local environment as the beta server is not always running on the latest version of EMX2. This means downloading necessary schemas and creating them in a clean database via gradle. To get started, follow these instructions.

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
