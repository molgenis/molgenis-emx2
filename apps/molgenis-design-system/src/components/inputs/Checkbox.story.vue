<template>
  <Story title="Inputs/Checkbox" :layout="{ type: 'grid', width: '75%' }">
    <Variant title="Single Checkbox">
      <Checkbox
        id="input-1"
        label="Do you agree to the terms and conditions?"
      />
    </Variant>
    <Variant title="Single Checkbox with description">
      <Checkbox
        id="input-2"
        label="Do you agree to the terms and conditions?"
      >
        <template v-slot:description>
          <p class="[&_a]:text-blue-500 [&_a]:underline [&_a]:decoration-1">By agreeing, you have read the <a href='#'>Terms and Conditions (pdf, 2mb)</a> and the <a href='#'>End Users Agreement (pdf, 0.9mb)</a></p>
        </template>
      </Checkbox>
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import Checkbox from "./Checkbox.vue";
</script>

<docs lang="md">
## Checkbox Input

The `<Checkbox>` component should be used in situtations where a boolean value is needed or a single value (i.e., value defined using the `value` attribute). It is also possible to provide additional context to the input by supplying a description.


### Usage

The following attributes are available.

| Name | Type | Required | Description | Default
|:-----|:-----|:-------- |:------------|:-------
| id   | string | `true` | a unique identifier for the checkbox | ---
| label | string | `true` | text that describes the checkbox | ---
| name | string | `false` | a name the links multiple checkboxes | ---
| value | string | `false` | a value that represents the checkbox | `true/false`
| checked | boolean | `true` | If true, the input will be checked by default | `false`

#### Slots

Use the `<template>` element and the following slot names to create the content in the form.


| Name | Vue Slot | Description
|:-----|:---------|:--------
| Description | `v-slot:description` | additional text that provides context for the input

### Examples

#### Default Checkbox

```js
<script setup lang="ts">
import Checkbox from "molgenis-design-system";
</script>

<template>
  <Checkbox id="newsletter-signup" label="Would you like to sign up for our newsletter?" />
</template>
```

#### Checkbox with description

```js
<script setup lang="ts">
import Checkbox from "molgenis-design-system";
</script>

<template>
  <Checkbox id="newsletter-signup" label="Would you like to sign up for our newsletter?" :checked="true">
    <template v-slot:description>
      <p>Receive the latest news and developments in your inbox.</p>
    </template>
  </Checkbox>
</template>
```

</docs>