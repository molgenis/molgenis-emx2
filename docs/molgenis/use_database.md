# Creating/managing databases

MOLGENIS allows you to create multiple databases on one server.

When you go to the root of your server you will see a listing of the databases. If you don't have permission to see any of
the databases then a warning will be shown.

If you cannot find it please go to https://servername/apps/central (Replace servername with your MOLGENIS instance)

If you have admin permissions you can create new databases using the '+' sign.

### Naming requirements
Each database name must be unique (among the database names on the server).

It must start with a letter, followed by zero or more letters, numbers, spaces, dashes or underscores. A space immediately before or after an underscore is not allowed. The character limit is 31.

Regular expression requirement: `^(?!.* _|.*_ )[a-zA-Z][-a-zA-Z0-9 _]{0,30}$`

Some examples:

| name                      | allowed |
|---------------------------|---------|
| camelCase                 | yes     |
| PascalCase                | yes     |
| with space                | yes     |
| with_underscore           | yes     |
| with_underscore and spaces | yes     |
| underscore_ space         | no      |
| space _underscore         | no      |
| space underscore at end _ | no      |
