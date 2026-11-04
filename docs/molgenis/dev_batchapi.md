# Batch web service API

Next go graphql API MOLGENIS comes with a batch API for large scale (meta)data upload.

You can see it all in action when you use the [up/download](use_updownload.md) tool.

TODO describe

* Excel
* CSV
* Yaml
* JSON
* RDF
* TTL

### Including system columns in download

By adding a query param ```includeSystemColumns=true``` to the api get request the system columns are included in the download.

#### example
```https://emx2.dev.molgenis.org/pet%20store/api/csv/Pet?includeSystemColumns=true```

return 
```
....
name,category,photoUrls,status,tags,weight,mg_draft,mg_insertedBy,mg_insertedOn,mg_updatedBy,mg_updatedOn
pooky,cat,,available,,9.4,,admin,2023-01-25 09:46:57.969716,admin,2023-01-25 09:46:57.969716
....
```

### Selecting columns in a table download

By adding a query param ```columns``` to a table download (csv or excel) only the listed columns are included, in the order given. Columns can be listed comma separated or as repeated params, by name or identifier. Only columns of the table itself are allowed, including columns it inherits from its parent tables. Dot-separated names are only allowed for a key part of a composite key reference (e.g. ```owner.firstName```). A reference column gives the key of the row it refers to: one column (e.g. ```pet``` holding the pet name), or one dot-separated column per key part when that key is composite (e.g. ```owner``` gives ```owner.firstName``` and ```owner.lastName```). A file column gives its file and filename columns. System columns listed explicitly are included without ```includeSystemColumns```. An unknown column gives a 400 error. Without the param all columns are downloaded.

#### example
```https://emx2.dev.molgenis.org/pet%20store/api/csv/Pet?columns=weight,name```

return
```
weight,name
9.4,pooky
....
```
