<template>
  <Story title="Inputs/Input Option" :layout="{ type: 'grid', width: '75%' }">
    <Variant title="Single Checkbox">
      <InputOption
        id="input-1"
        type="checkbox"
        label="Do you agree to the terms and conditions?"
      />
    </Variant>
    <Variant title="Single Checkbox with description and required">
      <InputOption
        id="input-2"
        type="checkbox"
        label="Do you agree to the terms and conditions?"
        :required="true"
      >
        <template v-slot:description>
          <p class="[&_a]:text-blue-500 [&_a]:underline [&_a]:decoration-1">
            By agreeing, you have read the
            <a href="#">Terms and Conditions (PDF, 2MB)</a> and the
            <a href="#">End Users Agreement (PDF, 0.9MB)</a>
          </p>
        </template>
      </InputOption>
    </Variant>
    <Variant title="Radio Inputs">
      <p class="font-semibold">Do you agree to the terms and conditions?</p>
      <InputOption
        type="radio"
        name="agreement"
        id="radio-yes"
        label="Yes, I do"
      />
      <InputOption
        type="radio"
        name="agreement"
        id="radio-no"
        label="No, I do not"
      />
      <p class="mt-2">
        <strong>Developers Note</strong>: You can use this component to
        manually create input groups, but it is strongly recommended to
        use the InputOptionGroup component instead.
      </p>
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import InputOption from "./InputOption.vue";
</script>

<docs lang="md">
## Input Option

The `<InputOption>` component should be used to create a single input option
such as a checkbox or a radio input. The checkbox input is useful if you 
would like to return a single value (i.e., true or false). However, the 
radio type should be used when there are two or more options to choose from.
If you have many options, you can manually create a group with this component
or you can use the `<InputOptionGroup>` component.

### Usage

The following attributes are available.

| Name     | Type    | Required | Description                                   | Default      |
| :------- | :------ | :------- | :-------------------------------------------- | :----------- |
| id       | string  | `true`   | a unique identifier for the checkbox          | ---          |
| type     | string  | `true`   | input type, `'checkbox' \| 'radio'`           | `checkbox`   | 
| label    | string  | `true`   | text that describes the checkbox              | ---          |
| name     | string  | `false`  | a name the links multiple checkboxes          | ---          |
| value    | string  | `false`  | a value that represents the checkbox          | `true/false` |
| checked  | boolean | `false`  | If true, the input will be checked            | `false`      |
| required | boolean | `false`  | If true, the input will be required           | `false`      |

#### Slots

Use the `<template>` element and the following slot names to create the content in the form.

| Name        | Vue Slot             | Description                                         |
| :---------- | :------------------- | :-------------------------------------------------- |
| Description | `v-slot:description` | additional text that provides context for the input |

### Examples

#### Default use

By default, the `<InputOption>` component will return a checkbox.

```js
<script setup lang="ts">
import InputOption from "molgenis-design-system";
</script>

<template>
  <InputOption
    id="newsletter-signup"
    label="Would you like to sign up for our newsletter?"
  />
</template>
```

#### Radio Input with description

```js
<script setup lang="ts">
import InputOption from "molgenis-design-system";
</script>

<template>
  <InputOption
    id="newsletter-signup"
    type="radio"
    label="Would you like to sign up for our newsletter?"
  >
    <template v-slot:description>
      <p>Receive the latest news and developments in your inbox.</p>
    </template>
  </InputOption>
</template>
```
</docs>
