# NESTOR Public

The `nestor-public` vue application contains the landing pages and dashboard for the NESTOR Registry.

## Getting Started

To create a new instance of the app, make a new schema using the `UI_DASHBOARD` template. The app will be accessible at `/<schema_name>/nestor-public`.

### Dashboard configuration

To poplate the dashboard, you will need to provide data for the following tables.

- Dashboard pages
- Charts
- Chart Data

Even though the dashboard updates when there are new records or changes in the data, the chart identifiers are hardcoded in the app. Use the following identifiers when creating the charts.

| chart                                    | chart type         | chart identifier              |
|------------------------------------------|--------------------|-------------------------------|
| map of organisations and data submission | map                | `organisation-map`            |
| enrollment by group                      | table              | `enrollment-by-disease-group` |
| assigned gender at birth                 | pie chart          | `sex-at-birth`                |
| age at last visit                        | vertical bar chart | `age-at-last-visit`           |

In the Chart Data table, import the data and link it to a chart using the column `included in chart`. For example:

| id          | name  | value |...| sort order | included in chart |
|-------------|-------|-------|---|------------|-------------------|
| age-group-1 | 20-29 | 5     |...| 1          | age-at-last-visit |

By default, sorting is enabled for most charts. You can specify the order of chart values by adjusting the `sort order` (see table above) for all records in a chart. Please note that the table and column charts are sorted in ascending order. The ordering of the pie chart cannot be adjusted to follow best data viz practices which is to sort in descending order and arrange the *slices* clockwise.

### Local development

When running locally, it is recommended to override the default proxy settings and point to the NESTOR development server. Create a `.env` file and add the following variables.

```text
MOLGENIS_APPS_HOST="..."
MOLGENIS_APPS_SCHEMA="..."
```
