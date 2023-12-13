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

* http://myserver.com/schema/api/report/1
* http://myserver.com/schema/api/report/1,2 (then you get result from two reports)
* http://myserver.com/schema/api/report/1,2?name=pooky,spike (then you get result from two reports using 'name' as parameter)