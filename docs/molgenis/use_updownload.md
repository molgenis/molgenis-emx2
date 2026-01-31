# Up and download tool

In the main menu choose 'up/download'.

You then will have the option to:

## Download

You can download data in the following formats:

* Excel
* CSV/ZIP (also includes file attachments)
* RDF
* TTl

You can download a [schema](use_schema.md) in the following formats:

* JSON
* YAML
* CSV

### Schema API endpoints

For programmatic schema access, use these REST endpoints:

**GET (download schema):**
```text
/{schema}/api/csv/_schema
/{schema}/api/json/_schema
/{schema}/api/yaml/_schema
/{schema}/api/excel/_schema
/{schema}/api/zip/_schema
```

**POST (merge schema):**
```text
/{schema}/api/csv/_schema
/{schema}/api/json/_schema
/{schema}/api/yaml/_schema
/{schema}/api/excel/_schema
/{schema}/api/zip/_schema
```

**DELETE (remove schema elements):**
```text
/{schema}/api/csv/_schema
/{schema}/api/json/_schema
/{schema}/api/yaml/_schema
/{schema}/api/excel/_schema
/{schema}/api/zip/_schema
```

Each table provides its own dedicated download links.

> **[Permissions](use_permissions.md):**  
> Only admin users or users with the **Owner** or **Manager** role are allowed to download the changelog and member data.

### Changelog

When downloading the changelog, you can control pagination using URL query parameters.

#### Query parameters
- **`limit`**: The maximum number of changelog entries to include. Defaults to 100.
- **`offset`**: The number of most recent entries to skip. Defaults to 0.

#### Example
```text
http://{SERVER_NAME}/{SCHEMA}/api/csv/_changelog?limit=200&offset=20
```
This request returns a CSV file with up to 200 changelog entries, excluding the 20 most recent ones.

## Upload

You can upload a [schema](use_schema.md) in:

* Excel
* CSV (_molgenis.csv_)
* JSON
* YAML

You can upload data in:

* Excel
* CSV+ZIP (including file attachments in subfolder _files)

It is possible to _delete_ rows from a table in EMX2 by using the upload functionality.
You can do so by adding a column named `mg_delete` to your Excel or CSV sheet and mark those rows as `true` in this
column for those you want to delete.

## FAQ

### How can I upload/download data when using columnType=file?

In case your schema contains columns with type=file then this column will be ommitted on download/upload unless you are
using `csv.zip`.
When using `csv.zip` then the files will be stored in a folder named _\_files_.
In your data tables you will see a reference to this file.
The file column should then contain the name of the file, without its extension.