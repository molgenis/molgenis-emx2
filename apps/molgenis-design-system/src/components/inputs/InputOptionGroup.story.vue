<template>
  <Story title="Inputs/Input Options Group" :layout="{ type: 'grid', width: '75%' }">
    <Variant title="Checkbox Group">
      <InputOptionGroup
        id="groupInput1"
        type="checkbox"
        title="How would like us to notify you?"
        name="group-1"
        :data="notificationOptions"
        row_id="id"
        row_label="type"
        row_value="type"
        :title_is_heading="true"
      >
        <template v-slot:description>
          <p>You can change your notification settings at any time.</p>
        </template>
      </InputOptionGroup>
    </Variant>
    <Variant title="Checkbox Group with descriptions and defaults">
      <InputOptionGroup
        id="groupInput2"
        type="checkbox"
        title="How would like us to notify you?"
        name="group-2"
        :required="true"
        :data="notificationOptions"
        row_id="id"
        row_label="type"
        row_value="type"
        row_description="info"
        row_checked="default"
        :title_is_heading="true"
      >
        <template v-slot:description>
          <p>You can change your notification settings at any time.</p>
        </template>
      </InputOptionGroup>
    </Variant>
    <Variant title="Radio Group">
      <InputOptionGroup
        id="radio-group-1"
        type="radio"
        title="How would like us to notify you?"
        name="radio-group-1"
        :data="notificationOptions"
        row_id="id"
        row_label="type"
        row_value="type"
        row_checked="default"
        :title_is_heading="true"
      >
        <template v-slot:description>
          <p>Choose your preferred method of contact. You can change your notification settings at any time.</p>
        </template>
      </InputOptionGroup>
    </Variant>
    <Variant title="Radio Group with descriptions">
      <InputOptionGroup
        id="radio-group-2"
        type="radio"
        title="How would like us to notify you?"
        name="radio-group-2"
        :required="true"
        :data="notificationOptions"
        row_id="id"
        row_label="type"
        row_value="type"
        row_description="info"
        row_checked="default"
        :title_is_heading="true"
      >
        <template v-slot:description>
          <p>Choose your preferred method of contact. You can change your notification settings at any time.</p>
        </template>
      </InputOptionGroup>
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import InputOptionGroup from "./InputOptionGroup.vue";

const notificationOptions = [
  {
    id: "n-01",
    type: "Emails",
    info: "Emails will be sent to my-test-email@gmail.com",
    default: true,
  },
  {
    id: "n-02",
    type: "Text",
    info: "Text messages will be sent to (***) *** 1234",
  },
  {
    id: "n-03",
    type: "Post",
    info: "You will receive paper copies to your home address",
  },
];
</script>

<docs lang="md">
## Input Option Group

The `<InputOptionGroup/>` component is useful for selecting one or more values
out of list of options. Options can be presented as a series of checkboxes or
radio inputs.

### Usage

The following attributes are available.

| Name            | Type    | Required | Description                                         | Default    |
| :-------------- | :------ | :------- | :-------------------------------------------------- | :--------- |
| id              | string  | `true`   | a unique identifier for the checkbox group          | ---        |
| type            | string  | `true`   | input type, `'checkbox' \| 'radio'`                 | `checkbox` | 
| title           | string  | `true`   | short text that describes the checkbox group        | ---        |
| name            | string  | `true`   | a unique name that groups all child checkboxes      | ---        |
| required        | boolean | `false`  | If true, the input will be required                 | `false`    |
| data            | array   | `true`   | an array of objects containing all checkbox options | ---        |
| row_id          | string  | `true`   | property that contains the ID                       | ---        |
| row_label       | string  | `true`   | property that contains the label                    | ---        |
| row_value       | string  | `false`  | property that contains the value                    | ---        |
| row_description | string  | `false`  | property that contains the description              | ---        |
| row_checked     | string  | `false`  | property that contains the checked status           | `false`    |
| title_is_heading | boolean | `false`  | If true, the title will be styled as a heading     | `false`    |

### Slots

Use the `<template>` element and the following slot name(s) to add additional context to the component.

| Name        | Vue Slot             | Description                                         |
| :---------- | :------------------- | :-------------------------------------------------- |
| Description | `v-slot:description` | additional text that provides context for the input |

### Examples

```js
<script setup lang="ts">
import InputOptionGroup from "molgenis-design-system";

const notificationOptions = [
  {
    id: "n-01",
    type: "Emails",
    info: "Emails will be sent to my-test-email@gmail.com",
    default: true,
  },
  {
    id: "n-02",
    type: "Text",
    info: "Text messages will be sent to (***) *** 1234",
  },
  {
    id: "n-03",
    type: "Post",
    info: "You will receive copies to your home address",
    default: true,
  },
];

</script>

<template>
  <InputOptionGroup
    id="users-notification-settings"
    type="checkbox"
    title="How would you like to be notified?"
    name="user-notifications"
    :data="notificationOptions"
    row_id="id"
    row_label="type"
    row_value="type"
    row_description="info"
    row_checked="default"
  >
    <template v-slot:description>
      <p>You can change your notification settings at any time.</p>
    </template>
  </InputOptionGroup>
</template>
```
</docs>
