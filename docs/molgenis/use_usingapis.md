# Using APIs

The EMX2 APIs can be used in various environments to retrieve, add, manipulate or replace data.



## Python

Data management of a Molgenis EMX2 server can be performed through the use of the Python client, see [Pyclient](use_usingpyclient.md).


## R

R is a free software environment for statistical computing and graphics.
Download the latest version from [CRAN](https://cran.r-project.org/).
A more powerful graphical environment such as [RStudio](https://www.rstudio.com/) can be helpful to work with R.

### Prerequisites

The `httr` package is used to connect to EMX2.
Install and load this package as follows:

```
install.packages("httr")
library(httr)
```

### Download data

Table data can be retrieved using GET and should be converted to a data frame before it can be used. Here we assume that the data has 'View' permissions for anonymous users.

```
pets <- GET(url = "https://joeri.molgeniscloud.org/pet%20store/api/csv/Pet")
petsdf <- read.csv(textConnection(content(pets, 'text', encoding = "UTF-8")))
```

Make a plot of pet weight and print the first row to check if everything looks right.

```
plot(petsdf$weight)
petsdf[1,]
```

Result:

```
   name category photoUrls    status tags weight
1 pooky      cat        NA available         9.4
```

### Alter data

Let's change pet weight from (supposedly) kilogram to grams by multiplying by one thousand. The new data should be converted back to CSV to prepare for upload.

```
petsdf$weight = petsdf$weight * 1000
newPetWeight <- capture.output(write.csv(petsdf))
```

### Upload data
The data can be uploaded to replace the original data using POST. Here we assume that anonymous users do not have 'Edit' permissions, so we must authenticate using a security token.
```
token <- "<your token here>"
header <- add_headers("x-molgenis-token" = token)
POST(url = "https://joeri.molgeniscloud.org/pet%20store/api/csv/Pet", body = newPetWeight, config = header)
```

If everything went well, a HTTP status code 200 (meaning [OK](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes)) is returned:

```
Response [https://joeri.molgeniscloud.org/pet%20store/api/csv/Pet]
  Date: 2023-04-05 09:38
  Status: 200
  Content-Type: text/csv
  Size: 1 B
```

Re-download the data to see if the weight value has been updated in the database.

```
newPets <- GET(url = "https://joeri.molgeniscloud.org/pet%20store/api/csv/Pet")
newPetsdf <- read.csv(textConnection(content(pets, 'text', encoding = "UTF-8")))
newPetsdf[1,]
```

Result:

```
   name category photoUrls    status tags weight
1 pooky      cat        NA available        9400
```

### Define new tables
todo

## Command line

The routes below work with any HTTP client. The examples use `curl`.

### Rows as JSON

The routes under `/{schema}/api/json` read and write table rows as JSON. You do not need to know GraphQL to use them.
The examples use the `pet store` schema. They set these variables first:

```
S='https://<server>/pet%20store/api/json'
```

Reads work for anonymous users that may view the table. Writes need a signed-in user. Sign in once and keep the token:

```
TOKEN=$(curl -s https://<server>/api/graphql -H 'Content-Type: application/json' \
  -d '{"query":"mutation{signin(email:\"<user>\",password:\"<password>\"){token}}"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["signin"]["token"])')
```

Send the token in the `x-molgenis-token` header of every request that needs it.

#### Write rows of one table

Send the rows as a JSON array to `/{schema}/api/json/{table}`. Each verb does what the matching [GraphQL mutation](dev_graphql.md#table-query-and-mutation-functions) does.

| Verb | Does | Example answer message |
|---|---|---|
| `POST` | Inserts new rows. An existing key fails. | `inserted 1 records to Pet` |
| `PUT` | Saves rows. It inserts new rows and replaces existing rows. | `upserted 1 records to Pet` |
| `PATCH` | Updates existing rows. It changes only the columns you send. | `updated 1 records to Pet` |
| `DELETE` | Deletes the rows with the keys you send. | `delete 1 records from Pet` |

A success answers 200 with a status and a message.

Insert a row:

```
curl -X POST "$S/Pet" -H "x-molgenis-token: $TOKEN" -H 'Content-Type: application/json' \
  -d '[{"name":"docs-example-1","category":{"name":"ant"},"status":"available","weight":1.5}]'
```

```
{"status":"SUCCESS","message":"inserted 1 records to Pet\n"}
```

Update one column of that row:

```
curl -X PATCH "$S/Pet" -H "x-molgenis-token: $TOKEN" -H 'Content-Type: application/json' \
  -d '[{"name":"docs-example-1","weight":2.5}]'
```

```
{"status":"SUCCESS","message":"updated 1 records to Pet\n"}
```

Save the whole row. `PUT` needs every required column, because it can insert the row. A column that you leave out of an existing row becomes empty:

```
curl -X PUT "$S/Pet" -H "x-molgenis-token: $TOKEN" -H 'Content-Type: application/json' \
  -d '[{"name":"docs-example-1","category":{"name":"ant"},"status":"pending","weight":3,"tags":[{"name":"red"}]}]'
```

```
{"status":"SUCCESS","message":"upserted 1 records to Pet\n"}
```

Delete the row:

```
curl -X DELETE "$S/Pet" -H "x-molgenis-token: $TOKEN" -H 'Content-Type: application/json' \
  -d '[{"name":"docs-example-1"}]'
```

```
{"status":"SUCCESS","message":"delete 1 records from Pet\n"}
```

A delete of a key that does not exist answers 200 and deletes 0 rows. Add `?strict=true` to fail instead:

```
curl -X DELETE "$S/Pet?strict=true" -H "x-molgenis-token: $TOKEN" -H 'Content-Type: application/json' \
  -d '[{"name":"docs-example-missing"}]'
```

```
{
  "errors" : [
    {
      "message" : "Transaction failed: Delete into table Pet failed: Delete failed: attempted to delete 1 rows but only deleted 0 rows. Some specified rows do not exist in table Pet. Transaction rolled back."
    }
  ]
}
```

The answer has status 400.

#### Write rows of several tables

The same four verbs on `/{schema}/api/json/_data` write several tables in one request. The body is a JSON object. Each key is a table id and each value is an array of rows.

One request is one transaction. If any row fails, no table changes. The order of the keys does not matter, because the server writes the tables in dependency order. This request lists `Pet` before the `Category` it refers to:

```
curl -X POST "$S/_data" -H "x-molgenis-token: $TOKEN" -H 'Content-Type: application/json' \
  -d '{"Pet":[{"name":"docs-example-2","category":{"name":"docs-example-category"},"weight":1}],"Category":[{"name":"docs-example-category"}]}'
```

```
{"status":"SUCCESS","message":"inserted 1 records to Category\ninserted 1 records to Pet\n"}
```

#### Read rows of one table

`GET /{schema}/api/json/{table}` returns a JSON array of rows. These parameters are optional:

| Parameter | Value |
|---|---|
| `filter` | URL-encoded JSON, in the [GraphQL filter syntax](dev_graphql.md#table-query-and-mutation-functions). |
| `orderby` | URL-encoded JSON, in the GraphQL order syntax, for example `{"weight":"DESC"}`. Without `orderby` the rows come in primary key order. |
| `search` | A text. The rows that contain it match. |
| `limit` | The number of rows to return. The default is 1000. |
| `offset` | The number of rows to skip. The default is 0. |
| `query` | The columns to return, as a GraphQL selection such as `{name,category{name}}`. |

`limit` below 1 and `offset` below 0 answer 400.

Without `query`, a row has the shape that a write takes. A reference is the key of the row it points to, for example `"category":{"name":"ant"}`. A file is an object with `id`, `size`, `filename`, `extension` and `url`. Refbacks and `mg_` columns are left out, except `mg_tableclass`.

The read examples ran while the rows `docs-example-1` and `docs-example-2` from the write examples existed.

Read the rows with a name that contains `docs-example`. `curl -G` with `--data-urlencode` does the URL encoding:

```
curl -G "$S/Pet" -H "x-molgenis-token: $TOKEN" \
  --data-urlencode 'filter={"name":{"like":"docs-example"}}'
```

```
[
  {
    "name" : "docs-example-1",
    "category" : {
      "name" : "ant"
    },
    "status" : "pending",
    "tags" : [
      {
        "name" : "red"
      }
    ],
    "weight" : 3.0
  }
]
```

Choose the columns with `query`:

```
curl -G "$S/Pet" -H "x-molgenis-token: $TOKEN" \
  --data-urlencode 'filter={"name":{"like":"docs-example"}}' \
  --data-urlencode 'query={name,category{name},weight}'
```

```
[
  {
    "name" : "docs-example-1",
    "category" : {
      "name" : "ant"
    },
    "weight" : 3.0
  }
]
```

#### Read the next page

A read returns at most 1000 rows, or the `limit` you give. If more rows remain, the answer has a `Link` header with `rel="next"`. The link is root-relative: it starts at the path, without the server name. The last page has no `Link` header.

This request asks for 2 rows and shows the headers:

```
curl -i -G "$S/Pet" -H "x-molgenis-token: $TOKEN" \
  --data-urlencode 'limit=2' --data-urlencode 'orderby={"name":"ASC"}'
```

```
HTTP/1.1 200 OK
Content-Type: application/json
Link: </pet%20store/api/json/Pet?limit=2&orderby=%7B%22name%22%3A%22ASC%22%7D&offset=2>; rel="next"
…
```

Request the link, with the server name in front, to get the next page.

Pages are cut by position. If you give an `orderby`, order by a column with unique values, or a row can show up on two pages or on none. Without `orderby` the primary key order is safe.

#### Read several tables

`GET /{schema}/api/json/_data` returns the first 1000 rows of every table you may view. The body is a JSON object with one array per table id. A table with more rows has its own `Link` header.

Add `query` to read what you choose. It must be a complete [GraphQL query](dev_graphql.md#table-query-and-mutation-functions). The answer is the result of the query. A `query` has no default page, and a mutation answers 400.

```
curl -G "$S/_data" -H "x-molgenis-token: $TOKEN" \
  --data-urlencode 'query={Pet(filter:{name:{like:"docs-example"}}){name,category{name}},Pet_agg(filter:{name:{like:"docs-example"}}){count}}'
```

```
{
  "Pet" : [
    {
      "name" : "docs-example-1",
      "category" : {
        "name" : "ant"
      }
    },
    {
      "name" : "docs-example-2",
      "category" : {
        "name" : "docs-example-category"
      }
    }
  ],
  "Pet_agg" : {
    "count" : 2
  }
}
```

#### Files

A read returns a file as an object. A write that sends that same object back keeps the file as it is. A write that sends `null` removes the file. Any object counts as "keep the stored file": its fields are not read, and for a new row it stores no file. A text value fails. These routes cannot store new file content. Upload files with the [CSV and ZIP routes](use_updownload.md).

#### Errors

A failed request changes nothing. It answers with status 400 and a body in this shape. The message tells you what to fix:

```
{"errors":[{"message":"…"}]}
```

An invalid row, a missing permission, a column that does not exist and a body of the wrong type all fail this way. For example, a `Pet` row without the required column `weight`:

```
curl -i -X POST "$S/Pet" -H "x-molgenis-token: $TOKEN" -H 'Content-Type: application/json' \
  -d '[{"name":"docs-example-1","category":{"name":"ant"}}]'
```

```
HTTP/1.1 400 Bad Request
Content-Type: application/json
…

{
  "errors" : [
    {
      "message" : "Transaction failed: Update into table 'Pet' failed.: column 'weight' is required in ROW(name='docs-example-1' category='ant' mg_tableclass='pet store.Pet')"
    }
  ]
}
```

#### Schema metadata and the API document

`/{schema}/api/json/_schema` answers GET, POST and DELETE as `/{schema}/api/json` does. It reads, changes and drops the schema metadata.

`/{schema}/api/openapi.yaml` is the generated OpenAPI document of all routes of the schema. `/{schema}/api/openapi` shows it in a browser.
