# Reports explorer

MOLGENIS allows creation of reports usig SQL. See pet store template for examples.

## Listing reports

Go to the reports app. If it is not on the menu that at end of address bar add '/reports'
You will get a listing of reports. Click on a report to view its results.

In addition you can select multiple reports and download them as zip file containing a CSV for each selected report.
The name of the report will be used to name the csv.

# Editing reports

If you have Manager or Admin permissions then you can also edit a report by clicking the pencil icon.
You will then be able to change the name and sql of the report.

# Using parameters

You can parameterize your queries using ```${name}``` or when you need strong typing ```${name:string_array}```
See the 'pet store' schema for an example.
The part behind the ':' should match a primitive column type in [Schema](use_schema.md) manual.