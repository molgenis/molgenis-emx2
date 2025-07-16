# Directory admin

This page provides documentation to help the administration of the directory application and data model.

## Settings

The directory has its own specific app settings which can be found by pressing the 'Settings' button (when logged in as admin) or going to: `http://yoururl.com/#/configuration`.

The JSON settings have the following options:

| option | default | description |
|--------|---------|-------------|
| language | "en" | Language of the app, needs configuration of the i18n setting to properly work. |
| graphqlEndpoint | "graphql" | Endpoint at the backend to which the query goes. |
| negotiatorType | "v3" | Version of the negotiator. Current options are `v1` for Podium or `v3` / `eric-negotiator` for the latest version. |
| negotiatorUrl | "https://negotiator.acc.bbmri-eric.eu/api/v3/requests" | URL of the negotiator. |
| negotiatorUsername | "" | Username for the negotiator, used if `v1` is set. |
| negotiatorPassword | "" | Password for the negotiator, used if `v1` is set. |
| biobankColumns | `see config file` | Columns to be shown on biobank cards. |
| biobankReportColumns | `see config file` | Columns to be shown in biobank reports. |
| collectionColumns | `see config file` | Columns to be shown in collection reports. |
| studyColumns | `see config file` | Columns to be shown in study reports. |
| filterFacets | `see config file` | Filters that users can set on the app. |
| biobankCardShowCollections | true | Wether to show collections on biobank cards. |
| landingpage | `see config file` | Configuration for setting a landing page. |
| pageSize | 12 | Number of biobanks shown per page. |
| i18n | `see config file` | Internationalisation settings. |
| banner | "" | Adds a banner at the top of the page. Supports HTML. |
| footer | "" | Adds a footer at the bottom of the page. Supports HTML. |
