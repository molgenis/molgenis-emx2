# Up and download tool

In the main menu choose 'up/download'.

You then will have the option to

## Download

You can download data in

* Excel
* Csv/Zip
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
* Csv (molgenis.csv)
* JSON
* YAML

You can upload data in:

* Excel
* Csv+zip

## FAQ

### How can I upload/download data when using columnType=file?

In case your schema contains columns with type=file then this column will be ommitted on download/upload unless you are
using csv.zip. When using csv.zip then the files will be stored in a folder named _files.