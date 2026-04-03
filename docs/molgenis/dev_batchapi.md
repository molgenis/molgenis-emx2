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
