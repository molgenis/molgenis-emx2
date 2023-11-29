<template>
  <Story title="Inputs/Checkbox Group" :layout="{ type: 'grid', width: '75%' }">
    <Variant title="Checkbox Group">
      <CheckboxGroup
        id="groupInput1"
        title="How would like us to notify you?"
        name="group-1"
        :data="notificationOptions"
        row_id="id"
        row_label="type"
        row_value="type"
      >
        <template v-slot:description>
          <p>You can change your notification settings at any time.</p>
        </template>
      </CheckboxGroup>
    </Variant>
    <Variant title="Checkbox Group with descriptions and defaults">
      <CheckboxGroup
        id="groupInput2"
        title="How would like us to notify you?"
        name="group-2"
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
      </CheckboxGroup>
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import CheckboxGroup from "./CheckboxGroup.vue";

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

<docs lang="md">
## Checkbox Group

The `<CheckboxGroup/>` component is useful for allowing users to select one or more related values.

### Usage

The following attributes are available.

| Name            | Type   | Required | Description                                         | Default |
| :-------------- | :----- | :------- | :-------------------------------------------------- | :------ |
| id              | string | `true`   | a unique identifier for the checkbox group          | ---     |
| title           | string | `true`   | short text that describes the checkbox group        | ---     |
| name            | string | `true`   | a unique name that groups all child checkboxes      | ---     |
| data            | array  | `true`   | an array of objects containing all checkbox options | ---     |
| row_id          | string | `true`   | property that contains the ID                       | ---     |
| row_label       | string | `true`   | property that contains the label                    | ---     |
| row_value       | string | `false`  | property that contains the value                    | ---     |
| row_description | string | `false`  | property that contains the description              | ---     |
| row_checked     | string | `false`  | property that contains the checked status           |

### Slots

Use the `<template>` element and the following slot names to create the content in the form.

| Name        | Vue Slot             | Description                                         |
| :---------- | :------------------- | :-------------------------------------------------- |
| Description | `v-slot:description` | additional text that provides context for the input |

### Examples

```js
<script setup lang="ts">
import CheckboxGroup from "molgenis-design-system";

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
  <CheckboxGroup
    id="users-notification-settings"
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
  </CheckboxGroup>
</template>
```
</docs>
