# Dashboard template for ERN Cranio providers

The `cranio-provider` vue app is a template for creating dashboards for data providers affiliated with ERN CRANIO. This app contains "mini dashboards" for each workstream and further breakdowns for ERN versus center summaries, as well as general and surgical overviews. As a result, the application is structured using nested router views.

```text
cranio-provider
    - Center landing page and overview
    - Craniosynostosis
        - General overview for all centers
        - Surgical overview for all centers
        - General overview for the current center
        - Surgical overview for the current center
    - Cleft lip and palate
        - Overview for the current center
        - Overview for all centers
    - Genetic Deafness (TBD)
    - Larynxcleft (TBD)
```

## Getting Started

Changes can be made to the application by cloning the `molgenis/molgenis-emx2` repository and creating a new branch; use the prefix `feat/...` or `fix/...` when naming the branch.

### Setting up your development instance

Select an EMX2 instance to use as your development instance. Log in as admin and create three schemas:

1. Public Schema: used to display the `cranio-public` vue application. Use the template `ERN_DASHBOARDS`. Give it a name e.g., `ErnStats`
2. Provider Schema: An organisation-level schema to display the `cranio-provider` vue application. Use one of the organisations listed in the [Cranio Organisations.csv](https://github.com/molgenis/projects-rd-erns/blob/main/erns/cranio/imports/organisations.csv) file. The name of the schema is listed in the column `schemaName`. For example, use `DK1` to create a schema for `Aarhus University Hospital`.
3. All Providers schema: A schema used to store the all site (or ERN wide) aggregations

After creating both schemas, navigate to the settings table in the provider schema. Here we will create a reference to the public schema to link the provider with public schemas. This allows the vue applications to be independent and to avoid hardcoding any schema information. The key is hardcoded.

| Key                    | Value                    |
|------------------------|--------------------------|
| CRANIO_PUBLIC_SCHEMA   | `<public_schema_name>`   |
| CRANIO_PROVIDER_SCHEMA | `<provider_schema_name>` |

Using the example schema name created in step one, we would use `ErnStats`.

### Updating the `vue.config.js` file

Open the `vue.config.js` file and update the following information.

1. set the `host`: enter the emx2 instance that you selected in the previous section
2. set the `schema`: enter the name of the provider schema created in the previous section (e.g., `DK1`)

### Importing the reference datasets

The datasets used in the dashboards are located in the [Cranio Imports folder](https://github.com/molgenis/projects-rd-erns/tree/main/erns/cranio/imports). To make the import process smoother, zip the folder and import it via the browser.

```bash
cd erns/cranio/imports
zip -r ../ern_cranio.zip *
```

In the terminal, change directories to `apps/cranio-provider` and run the `yarn dev` command. This will serve the vue app at `localhost:5173`

## Troubleshooting

1. **Error importing components**: If you get an error importing components from `molgenis-viz` or `molgenis-components`, these may need to be built and installed locally. In each folder, run `yarn build`
