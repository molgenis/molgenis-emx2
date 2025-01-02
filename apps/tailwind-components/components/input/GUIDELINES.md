## Guidelines for inputs

Inputs:

- shouldn't have margins, that is up to the wrapper to apply
- should have 'script' before 'template' so the props are on top of the file

All inputs should have the following props:

- id
- modelValue
- inverted

All inputs could have the following props:

- placeholder
- hasError
- valid
- disabled
- schemaId (for backend linked inputs)
- tableId (for backend linked inputs)

N.B. the following files are not inputs and should be moved elsewhere

- Label
- Placeholder
