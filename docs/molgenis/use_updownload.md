# Up and download tool

In the main menu choose 'up/download'.

You then will have the option to

## Download

You can download data in

* Excel
* CSV/ZIP (also includes file attachments)
* RDF
* TTl

You can download a [schema](use_schema.md) in

* JSON
* YAML
* CSV

In addition you get download links per table.

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
You can do so by adding a column named `mg_delete` to your Excel or CSV sheet and mark those rows as `true` in this column for those you want to delete.

## FAQ

### How can I upload/download data when using columnType=file?

In case your schema contains columns with type=file then this column will be ommitted on download/upload unless you are using `csv.zip`. 
When using `csv.zip` then the files will be stored in a folder named _\_files_. 
In your data tables you will see a reference to this file. 
The file column should then contain the name of the file, without its extension.