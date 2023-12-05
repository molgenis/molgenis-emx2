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
        error="Search term cannot be blank"
      />
    </Variant>
    <Variant title="Type - Search">
      <TextInput
        id="type-search"
        label="Search"
        type="search"
      />
    </Variant>
    <Variant title="Type - Email">
      <TextInput
        id="type-email"
        label="Enter your email"
        type="email"
      />
    </Variant>
    <Variant title="Type - Password">
      <TextInput
        id="type-password"
        label="Enter your password"
        type="password"
      />
    </Variant>
    <Variant title="Type - Tel">
      <TextInput
        id="type-tel"
        label="Enter your telephone number"
        type="tel"
      />
    </Variant>
    <Variant title="Type - Url">
      <TextInput
        id="type-url"
        label="Enter a URL"
        type="url"
      />
    </Variant>
    <Variant title="Type - Number">
      <TextInput
        id="type-number"
        label="Enter a number"
        type="number"
      />
    </Variant>
    <Variant title="Type - Date">
      <TextInput
        id="type-date"
        label="Enter a date"
        type="date"
      />
    </Variant>
    <Variant title="Type - Datetime local">
      <TextInput
        id="type-datetime-local"
        label="Enter a date and time"
        type="datetime-local"
      />
    </Variant>
    <Variant title="Type - Time">
      <TextInput
        id="type-time"
        label="Enter a time"
        type="time"
      />
    </Variant>
  </Story>
</template>
<script setup lang="ts">
import TextInput from "./InputText.vue";

const label = "Search for patients";
const placeholder = "'A-001' or 'A-001,A-002'";
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
| type | string  | `true`   | text input type, `'date' \| 'datetime-local' \| 'email' \| 'number' \| 'password' \| 'search' \| 'tel' \| 'text' \| 'time' \| 'url'` | `'text'` |
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