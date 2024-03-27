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

### Vue config

A few items are required to develop the application. These are outlined below.

1. In the `vue.config.js` file, enter a host or schema, or use the defaults.
2. In the selected host, create a new schema using the template `ERN_DASHBOARDS`. Use the name `CranioStats`. **NOTE**: the name of this schema is hard coded into the Cranio provider application.
3. Upload the [Cranio dataset](https://github.com/molgenis/projects-rd-erns/blob/main/erns/cranio/cranio_emx2.xlsx) into the `CranioStats` schema.
4. In the organisations table, select one of the organisations and create a new schema using the value in the `providerInformation` column (e.g., `NL1`, `DK1`, etc).
5. Next we will import the profile image for the organization. Click the "edit row" button and scroll down to the field for `image`. Find the profile image of the selected organisation in the [provider profiles](https://github.com/molgenis/projects-rd-erns/tree/main/erns/cranio/profiles) folder. (Either download the image or clone the repo.) Back in the update form, click the *browse* button and select the file that you would like to import. Click save.
6. In the terminal, change directories to `apps/cranio-provider` and run the `yarn dev` command. This will serve the vue app at `localhost:5173`

## Troubleshooting

1. **Error importing components**: If you get an error importing components from `molgenis-viz` or `molgenis-components`, these may need to be built and installed locally. In each folder, run `yarn build`
