# Reports explorer

MOLGENIS allows creation of reports using SQL. See pet store template for examples.

## Listing reports

Go to the reports app. If it is not on the menu that at end of address bar add 'yourschema/reports'
You will get a listing of reports. Click on a report to view its results.

In addition you can select multiple reports and download them as zip file containing a CSV for each selected report.
The name of the report will be used to name the csv.

# Editing reports

If you have Manager or Admin permissions then you can also edit a report by clicking the pencil icon.
You will then be able to change the name and sql of the report.

Example of a report:

```select * from "Pet"```

Each report requires an unique id to retrieve, and an optional human readable description.

n.b. since version 12 the 'id' is required and used, please update reports when you created them before 12. Technically reports are stored as json into 
settings under key 'reports', for example: 

```
[
   {
      "id":"report1",
      "description":"pet report",
      "sql":"select * from \"Pet\""
   },
   {
      "id":"report2",
      "description":"pet report with parameters",
      "sql":"select * from \"Pet\" p where p.name=ANY(${name:string_array})"
   },
]
```

# Using parameters

You can parameterize your queries using ```${name}``` or when you need strong typing ```${name:string_array}```
See the 'pet store' schema for an example.
The part behind the ':' should match a primitive column type in [Schema](use_schema.md) manual.

Example of a report that has a parameter:

```select * from "Pet" p where p.name=ANY(${name:string_array})```

In this query the user interface will show a dialogue to enter one or more name. The backend will then convert this to an array of string before inserting 
it into SQL.

# Using the api instead of the user interface

You can also use these reports in scripts, to directly download the results, for example:

* http://myserver.com/schema/api/report/report1
* http://myserver.com/schema/api/report/report1,report2 (then you get result from two reports)
* http://myserver.com/schema/api/report/report2,report3?name=pooky,spike (then you get result from two reports using 'name' as parameter)

# Returning JSON so you can use reports as REST like 'get' API

You can create queries returning JSON to create JSON results consisting of nested data. If there is only one JSON result per row that value will be used in 
result set. For example:
```SELECT to_jsonb("Pet") AS result FROM "Pet"```

Results in something like: 
```[{name= ...},{name=...}]```

N.B. when you ask for result from multiple reports you will get a nested result using the report names as keys, i.e.
```{report1:[{...}], report2:[{...}]}```