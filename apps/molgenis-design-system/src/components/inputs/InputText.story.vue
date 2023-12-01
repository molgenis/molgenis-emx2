<template>
  <Story title="Inputs/Text" :layout="{ type: 'grid', width: '400px' }">
    <Variant title="Default">
      <TextInput
        id="search-patients-default"
        :label="label"
        :placeholder="placeholder"
      />
    </Variant>
    <Variant title="Disabled">
      <TextInput
        id="search-patients-disabled"
        :label="label"
        :placeholder="placeholder"
        :disabled="true"
      />
    </Variant>
    <Variant title="Required">
      <TextInput
        id="search-patients-required"
        :label="label"
        :placeholder="placeholder"
        :required="true"
      >
        <template v-slot:description>
          <p class="text-gray-600/80">Search for patient identifier, affiliation, etc.</p>
        </template>
      </TextInput>
    </Variant>
    <Variant title="Valid">
      <TextInput
        id="search-patients-valid"
        :label="label"
        :placeholder="placeholder"
        :valid="true"
      />
    </Variant>
    <Variant title="Error">
      <TextInput
        id="search-patients-error"
        :label="label"
        :placeholder="placeholder"
        :has-error="true"
        error="Search term cannot be blank"
      />
    </Variant>
  </Story>
</template>
<script setup lang="ts">
import TextInput from "./InputText.vue";

const label = "Search for patients";
const placeholder =
  "Enter one or more identifiers; e.g., 'A-001' or 'A-001,A-002'";
</script>

<docs lang="md">
## Text Input

The `<TextInput>` component is used to capture any user input, such as 
text, emails, password, etc. 

### Usage

The following attributes are available.

| Name            | Type    | Required | Description                                         | Default    |
| :-------------- | :------ | :------- | :-------------------------------------------------- | :--------- |
| id   | string  | `true`   | a unique identifier for the checkbox group          | ---        |
| type | string  | `true`   | text input type, `'text' \| 'search' \| 'number' \| 'email' \| 'password' \| 'tel' \| 'url'` | `'text'` |
| label | string | `true` | text that describes the input | --- 
| placeholder | string | `false` | placeholder text to display | ---
| disabled | boolean | `false` | if true, the input will be disabled | `false` |
| required | boolean | `false` | if true, the input will be required | `false` |
| valid | boolean | `false` | if true, the input will be styled as valid | `false` |
| error | string | `false` | display an error message if there is a form validation issue | ---

#### Slots

Use the `<template>` element and the following slot names to create add content in the form.

| Name        | Vue Slot             | Description                                         |
| :---------- | :------------------- | :-------------------------------------------------- |
| Description | `v-slot:description` | additional text that provides context for the input |


### Examples

```js
<script setup lang="ts">
import TextInput from "molgenis-design-system";
</script>

<template>
  <TextInput
    id="search-patients-required"
    label="Search for patients"
    placeholder="'A123' or 'A123, A456'"
    :required="true"
  >
    <template v-slot:description>
      <p class="text-gray-600/80">
        Search for one or more patient identifiers
      </p>
    </template>
  </TextInput>
</template>
```


</docs>