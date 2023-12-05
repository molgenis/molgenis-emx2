<template>
  <Story title="Inputs/Select" :layout="{type: 'grid', width: '400px'}">
    <Variant title="Default">
      <SelectInput 
        id="default-options"
        label="Select an option"
        :options="options"
        option_label="name"
        option_value="code"
      />
    </Variant>
    <Variant title="Required">
      <SelectInput 
        id="default-options"
        label="Select an option"
        :required="true"
        :options="options"
        option_label="name"
        option_value="code"
      />
    </Variant>
    <Variant title="Disabled">
      <SelectInput 
        id="default-options"
        label="Select an option"
        :disabled="true"
        :options="options"
        option_label="name"
        option_value="code"
      />
    </Variant>
    <Variant title="Valid">
      <SelectInput 
        id="default-options"
        label="Select an option"
        :valid="true"
        :options="options"
        option_label="name"
        option_value="code"
      />
    </Variant>
    <Variant title="Error">
      <SelectInput 
        id="default-options"
        label="Select an option"
        error="No option selected"
        :options="options"
        option_label="name"
        option_value="code"
      />
    </Variant>
    <Variant title="Error With description">
      <SelectInput 
        id="default-options"
        label="Select an option"
        :options="options"
        option_label="name"
        option_value="code" 
      >
        <template v-slot:description>
          <span class="text-gray-600">Options range from A to C</span>
        </template>
      </SelectInput>
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import SelectInput from "./InputSelect.vue";

const options = [
  { code: 'A', name: 'Option A'},
  { code: 'B', name: 'Option B'},
  { code: 'C', name: 'Option C'},
]
</script>


<docs lang="md">
## Select Input

The `SelectInput` is designed to present a list of options as a dropdown menu.
Alternatively, you can use the `<InputOptionsGroup>` component.

### Usage

The following attributes are available.

| Name            | Type    | Required | Description                                         | Default    |
| :-------------- | :------ | :------- | :-------------------------------------------------- | :--------- |
| id              | string  | `true`   | a unique identifier for the select input          | ---        |
| label | string | `true` | text that describes the input | --- |
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
<script setup>
import SelectInput from "molgenis-design-system";

const choices = [
  { name: 'Group A', code: 'g-a' }
  { name: 'Group B', code: 'g-b' }
  { name: 'Group C', code: 'g-c' }
  { name: 'Group D', code: 'g-d' }
]
</script>

<template>
  <SelectInput
    id="mySelectInput"
    label="Select an option"
    :options="choices"
    option_label="name"
    option_value="code"
  />
</template>

```

</docs>