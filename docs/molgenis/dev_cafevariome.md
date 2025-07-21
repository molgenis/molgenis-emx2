# Cafe Variome Integration

EMX2 provides integration with [**Cafe Variome**](https://v3doc.cafevariome.org/developer/cafe-variome-v3.html), a flexible, web-based data discovery
tool designed for biomedical data owners. It enables users to **discover the existence of data without revealing its
substance**. Supporting various data formats—such as **Phenopackets, XLSX, CSV**, and more—Cafe Variome provides a
unified interface to query patient records efficiently and securely.

## Authentication Configuration

To enable authentication with Cafe Variome in EMX2, you must configure the following **Keycloak-specific** settings.
These are **not** the same as the standard OIDC settings in EMX2:

- `CV_CLIENT_ID` — Client ID registered with the Cafe Variome authentication server.
- `CV_CLIENT_SECRET` — Secret associated with the above client ID.
- `CV_INTROSPECT_URI` — URI used for token introspection in the authentication flow.

When a user authenticates via Cafe Variome, EMX2 automatically matches the authenticated user to an existing EMX2 user
account using the information retrieved from Keycloak. Allowing you to configure access permissions in EMX2.

## Data loading

Like the Beacon endpoint, the Cafe Variome APIs are automatically exposed when loading the `PATIENT_REGISTRY` data
template. See the [Beacon](dev_beaconv2.md) documentation for instructions on how to enable this.

## Query Endpoints

Two primary Cafe Variome query endpoints are implemented in EMX2:

### 1. Record Index

```/[schema]/api/cafevariome/record-index```
Use this GET endpoint to retrieve the available indices at the record level.

The `record-index` endpoint exposes the **querying capabilities** of EMX2 for a given schema. It returns a description
of the attributes that can be queried via Cafe Variome, allowing Cafe Variome nodes to understand how to build
valid queries.

For more details on the structure and semantics of this response, see
the [Cafe Variome documentation on record-level indices](https://v3doc.cafevariome.org/developer/data-indices.html#record-level-indices).

### 2. Record Query

This POST endpoint is used to query individual records in Cafe Variome, using the filters provided in the `record-index`
response

```/[schema]/api/cafevariome/record```

Currently only `count` and `boolean` responses are implemented.

Example count query:
```json
{
  "subject": {
    "affectedOnly": false,
    "ageFirstDiagnosis": {
      "min": 18,
      "max": 65
    },
    "gender": "female"
  },
  "hpo": [
    {
      "terms": [
        "1955"
      ]
    }
  ],
  "advanced": {
    "granularity": "count"
  }
}
```

Example boolean query:
```json
{
 "hpo": [
    {
      "terms": [
        "1955"
      ]
    }
  ],
  "advanced": {
    "granularity": "boolean"
  }
}
```

