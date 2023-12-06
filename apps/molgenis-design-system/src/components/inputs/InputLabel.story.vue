<template>
  <Story title="Inputs/Input Label" :layout="{ type: 'grid', width: '500px' }">
    <Variant title="Default"> 
      <InputLabel id="label-default" label="Search" />
    </Variant>
    <Variant title="With Description"> 
      <InputLabel
        id="label-description"
        label="Search"
        description="Enter one or more search terms."
      />
    </Variant>
    <Variant title="As heading and with description"> 
      <InputLabel
        id="label-description"
        label="Search"
        description="Enter one or more search terms"
        :label_as_heading="true"
      />
    </Variant>
    <Variant title="Required"> 
      <InputLabel
        id="label-required"
        label="Search"
        :required="true"
      />
    </Variant>
    <Variant title="Valid"> 
      <InputLabel
        id="label-valid"
        label="Search"
        :valid="true"
      />
    </Variant>
    <Variant title="Error"> 
      <InputLabel
        id="label-error"
        label="Search"
        :error="true"
      />
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import InputLabel from './InputLabel.vue';
</script>

<docs lang="md">
## Input Label

The `<InputLabel>` component provides context for an input element by
providing space for a label and an optional description. It is primarily
intended for use in other input components, but it can be used in custom
interfaces.

These text elements were seperated from the input in order to standarise
the rendering of labels and descriptions, to centralise state-based
styling (e.g., is valid, errors, etc.), and to allow for more flexibility
in the display of the label when used in other situations (i.e., ensure
web accessibility).

### Usage

The following attributes are available.

| Name            | Type    | Required | Description                                         | Default    |
| :-------------- | :------ | :------- | :-------------------------------------------------- | :--------- |
| id   | string  | `true`   | the idenitifier of the input, i.e., the `for` attribute | --- |
| label | string | `true` | text that describes the input | --- |
| disabled | boolean | `false` | if true, the input will be disabled | `false` |
| required | boolean | `false` | if true, the input will be required | `false` |
| valid | boolean | `false` | if true, the input will be styled as valid | `false` |
| error | string | `false` | display an error message if there is a form validation issue | --- |
| label_as_heading | string | `false` | If true, the label will be stylised as a heading element | `false` |

When implementing this component in other form input components, it is important
to link the description to the input using the identifier of the description.
The ID is automatically generated using the pattern `input-description-${id}`
where `id` is defined at the instance level. This value should be added to
the input using the `aria-describedby`.

### Examples

```js
<script setup>
import InputLabel from "molgenis-design-system";
</script>

<template>
  <InputLabel id="search" label="Search" description="Enter or more patient identifiers" />
  <input
    id="search"
    aria-describedby="input-description-search"
    ...
  />
</template>

```

</docs>