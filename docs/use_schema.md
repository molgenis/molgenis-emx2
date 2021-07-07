# Schema definition

A MOLGENIS [database](use_database.md) is defined by its schema. The schema enables rich data modeling using tables,
columns and relationships.

You can create complete custom database schema's using molgenis schema editor or by uploading 'EMX2' schema file format.
Therefore in Excel file create a sheet named `molgenis`. Or create a molgenis.csv or molgenis.tsv sheets and upload as
part of a zip file. Note: you can upload the data as well in same file For that make sure each Excel sheet or .tsv/.csv
file is named in your molgenis metadata sheet.

## Example 'molgenis' schema

| tableName | columName  | type | key | required | description               |
|-----------|------------|------|-----|----------|---------------------------|
| Person    |            |      |     |          | my person table           |
| Person    | id         | int  | 1   |          | id is part of primary key |
| Person    | firstName  |      | 2   |          |                           |
| Person    | lastName   |      | 2   |          |                           |

Note: combination of Person.firstName + Person.lastName is unique in table Person.

## Simple columns

You can describe basic columns using:

### tableName

Will be name of table. Must start with one of a-zAZ followed by zero or more of _a-zAZ1-3. Maximumn length 31
characters. If you leave columnName empty then all other settings will apply to table instead of column

### columnName

Will be name of column. Must be unique per tableName. Must start with one of a-zAZ followed by zero or more of _
a-zAZ1-3. Maximum length 31 characters. Default value: empty

### columnType

Will be type of column. Ignored if columnName is empty. See section on columnTypes below. Default value: string MOLGENIS
supports the following types (type names are case insensitive):

Basic type:

* string : default when no type is provided
* bool
* int
* decimal
* uuid
* jsonb : validates json format
* file
* text_array : string that displays as text area

Relationships:

* ref : foreign key (aka many to one)
* ref_array : multiple foreign key (aka many to many)
* refback : to describe link back to ref/ref_array (aka one_to_many/many_to_many)

Arrays (i.e. list of values)

* string_array
* bool_array
* int_array
* decimal_array
* date_array
* datetime_array
* jsonb_array
* uuid_array
* text_array

### key

Will indicate that a column is part of a key. Ignored if columnName is empty. Key means values in this column should be
unique within the table. When key=1 this is used as primary key in user interface, upload and API Other key>1 can be
used to create secondary key. Default value: empty

### required

When required=TRUE then values in this column must be filled When required=FALSE then this column can be left empty.
Default value: FALSE.

### description

Text value that describes column, or when columnName is emtpy, the table.

## Cross-references

You can define cross-reference from one table to another using columnType=ref (single reference) or
columnType=ref_array (multiple references). In postgresql these translate to foreign keys, and array of foreign key with
triggers protecting foreign key constraints respectively. You need to define refTable, and optionally refFrom, refTo.

### refTable

This metadata is used to define relationships between tables. When columnType is 'ref' or 'ref_array' then you must
provide refTable. In simple cases, this is all you need. The value of refTable should a defined tableName. Default
value: empty

A simple reference:

| tableName | columName  | type | key | refTable | required | description               |
|-----------|------------|------|-----|----------|----------|---------------------------|
| Person    |            |      |     |          |          | my person table           |
| Person    | id         | int  | 1   |          |          | id is part of primary key |
| Person    | firstName  |      | 2   |          | TRUE     |                           |
| Person    | lastName   |      | 2   |          | TRUE     |                           |
| Pet       | name       |      | 1   |          |          |                           |   
| Pet       | species    |      |     |          | TRUE     |                           |
| Pet       | owner      | ref  |     | Person   | TRUE     | foreign key to Person     |

Note: when key=1 then automatically required=TRUE

### refBack

In case you want to create a two-directional relationship you can use columnType=refback + refBack=aColumnName. The
refBack should then refer to a column in refTable that is of columnType=ref or columnType=ref_array and refers to this
table. A refback column behaves as a ref_array, but is in fact either many_to_many or many_to_one, depending if the
refback is ref or ref_array. Refback columns are readonly (i.e. you cannot insert/update data in these columns). See
example below.

### refFrom, refTo

When refTable has multiple primary key columns (i.e. column with key=1) then you must also define how you want to name
the fields that are part of this column. The values in refTo must match the primary key columns of refTable. The values
in refFrom can be chosen freely, but must be unique. Optionally, you can name them same as existing columnName, but only
if the relationships overlap. See example below:

Example of complex relationships:

| tableName | columName  | type    | key | refTable | refFrom         | refTo              | refBack | required | description                 |
|-----------|------------|---------|-----|----------|-----------------|--------------------|----------|----------|-----------------------------|
| Person    |            |         |     |          |                 |                    |          |          | my person table             |      
| Person    | firstName  |         | 1   |          |                 |                    |          |          |                             |
| Person    | lastName   |         | 1   |          |                 |                    |          |          |                             |
| Person    | pets       | refback |     | Pet      |                 |                    | owner    |          |                             |
| Pet       | name       |         | 1   |          |                 |                    |          |          |                             |   
| Pet       | species    |         |     |          |                 |                    |          |          |                             |
| Pet       | owner      | ref     |     | Person   | ownerFN,ownerLN | firstName,lastName |          |          | multi-foreign key to Person |

## Expressions (alpha/planned)

You can futher finetune behavior of tables using javascript expressions. NOT YET FULLY IMPLEMENTED

### computedValue

Enables to compute a value. Computed values are computed before a record is inserted/updated. The computedColumn must
contain valid javascript returing the value. All columns of the table are available as variable. Computed values are
readonly in the user interface.

For example:

| tableName | columnName | key | computedValue            |
|-----------|------------|-----|--------------------------|
| parts     | id         | 1   | productNo + "_" + partNo |
| parts     | productNo  | 2   |                          |
| parts     | partNo     | 2   |                          |

### validation expression, visible expression

Validation expressions and visible expressions are used to finetune forms. Validation expressions must be valid
javascript. Validation expression must return null, otherwise will show error message and prevents insert/update.
Visible expressions must return true, otherwise column stays hidden in user interface. In case javascript throws an
exception, this is show in user interface/error message.

Example:

| tableName | columnName | type | key | validation                                       | visible            |
|-----------|------------|------|-----|--------------------------------------------------|--------------------|
| person    | id         |      | 1   |                                                  |                    |
| person    | birth      | date |     | if(birth > death) 'birth should be before death' |                    |
| death     | death      | date |     | if(birth > death) 'death should be after death'  | birth != undefined |

## Table inheritance

You can reuse table definitions, and make more specialised tables using 'tableExtends'.

### tableExtends

Should contain value of existing tableName. When providing tableExtends, the column with 'columnName' should be empty.
It means the columns defined in that tableName will be added to this table. In addition, rows added to this table, will
also be visible in the table that is extended.

## Cross schema references/extends

Usually it is good practice to keep all tables you work with in one schema, so you can upload/download as a unit.
However, there might be cases where you want to refer to data in other schemas. For example, large reference sets, or
situation where you have multiple organisations contributing data. For those cases you can use 'refSchema'

### refSchema

Using meta data 'refSchema' you can indicate that a refTable or tableExtends should look into other schema. In addition,
binding to a particular schema greatly limits flexibility of the data structure. An additional requirement is that the
table from the other schema should not have name conflict with any table in current schema. Because practically, the
table from the other schema will be imported into current schema.

## FAQ

Q: Do you support automatic values, such as autoincrement identifiers or dates?

A: No. We believe scientific data must be reproducible. Therefore we want to make sure that when you upload+download
data twice, the contents is exactly the same. This would break in case of automatic values. In case you really want it,
you can use computedValue.

