# Getting started with empty MOLGENIS

MOLGENIS enables you to quickly create databases, batteries included. This guide explains how to get started. We assume
you just installed MOLGENIS (see installation guide).

## 1. Log in as admin

After you start MOLGENIS you will see screen below. You can "sign in" using button right top, choosing username 'admin'
and password 'admin'.

![](img/start-welcome.png)

## 2. Change admin password

First thing to do is to change the admin password. Therefore you can click your username in menu bar on top, which
currently should be 'Hi admin' and then you can change password.

![](img/start-change-password.png)

## 3. Browse databases

MOLGENIS comes with a 'pet store' example database. You can browse into it to see how it looks like. And you can always
click the [molgenis] logo to go back to this screen.

![](img/start-database-list.png)

## 3. Create database

MOLGENIS enables you to create databases. Two databases do exists (1) an example database called 'pet store' and (2) a
database with System settings. Click the blue [+] icon atop the list of databases.

![](img/start-create-database.png)

## 4. Go to schema

Once a database is created, MOLGENIS enables you to configure it. You can do this using the 'schema' editor, or by
uploading a previously configured 'emx2' file (which is a great way to share best practices). We will go to edit '
schema'.

![](img/start-create-database-next.png)

## 5. Edit schema

MOLGENIS power is a complete freedom of data model, consisting of tables and columns. You can use the [+] to create new
tables. And you can use the [pencil] symbol to edit.

![](img/start-schema-edit.png)

## 6. Create tables

Important notes:

- each table should have unique name
- each table should have at least one column
- each table should have primary key, indicate by setting '1' to the key column
- tables can be linked via a column of type 'REF' or 'REF_ARRAY' (i.e. foreign key)

Enter the example below and click 'save' to create your schema.
![](img/start-schema-edit-example.png)

## 7. View tables data

Go to 'tables' on main menu to see your tables.

![](img/start-tables.png)

## 8. Enter data

Click on the name a table to view its contents. The result might look like this.

![](img/start-tables-example.png)

## 9. Download your data + schema

You might to learn how to upload/download data. For this go to upload/download on the main menu. Then you can download
all or subsets of your data (i.e. table contents) and metadata (i.e. schema) in various formats.

![](img/start-download.png)

## 10. View Excel download

Click 'Export all data as excel' and open the file. You will see that both your data (contents of Authors and Posts)
and metadata (table and column definitions) is included.

![](img/start-download-excel.png)

## 11. Upload in new schema

You can upload these contents into a new database, to create a 'clone' of your database. Therefore create a new
database (see above) and then go to 'upload files' for your new database and browse to the file you just downloaded.
Press '
import'.

![](img/start-import.png)

This completes the 'getting started guide'. Go to view EMX2 reference to learn full details of data modeling.







