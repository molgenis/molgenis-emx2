# MOLGENIS platform user manual

MOLGENIS enables you to quickly create databases, batteries included. We recommend the [quick start](use_quickstart.md)
to get a flavor of what MOLGENIS data platform provides.

Basic concepts:

* MOLGENIS is organized into [databases](use_database.md). You can consider a database as a closed workspace for your
  projects or applications. Optionally you can create cross-references between databases.
* Each database is structured using its [schema](use_schema.md). The schema models tables and columns, their
  relationships and constraints, and optionally FAIR semantics and hints on formats/how to display.
* Based on the schema, MOLGENIS automatically generates [forms](use_forms.md) to manage contents into this schema, i.e.
  to enable users to enter and manage data rows in the tables.
* Based on schema + contents, MOLGENIS generates rich tools to query [tables](use_tables.md), where you can use advanced
  filters and selections to drill down into the data.
* For large scale data management, batch upload of schema and/or contents (tables and complete database) is available
  using [up/download](use_updownload.md) tool. Currently, we support up/download using Excel, csv, zip+csv, json, yaml,
  rdf and ttl.
* Access to databases is controlled by registering [members](use_roles.md). Members are users that have a role within
  the database; the role defines user permissions to view, edit, or even manage schema+settings.
* Each schema can be customized with theme colors, logo, custom pages and more using
  database [settings](user_database_settings.md)



