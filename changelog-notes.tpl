# Changelog


{{#releases}}
## Release [{{name}}](https://github.com/molgenis/molgenis-emx2/releases/{{name}}) ({{date}})

{{#sections}}
### {{name}}

{{#commits}}
* {{message.shortMessage}} [{{#short7}}{{sha}}{{/short7}}](https://github.com/molgenis/molgenis-emx2/commit/{{#short7}}{{sha}}{{/short7}}) {{#capture expression="r(?i)closes\s+#[0-9]+(?:\s+#[0-9]+)*" group="1"}}{{message.fullMessage}}{{/capture}}

{{/commits}}
{{^commits}}
No changes.
{{/commits}}
{{/sections}}
{{^sections}}
No changes.
{{/sections}}
{{/releases}}
{{^releases}}
No releases.
{{/releases}}
