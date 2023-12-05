<template>
  <Story title="Icons/Input State" :layout="{ type: 'grid', width: '400px' }">
    <Variant title="Default (no icon)">
      <InputStateIcon />
    </Variant>
    <Variant title="Valid">
      <InputStateIcon :valid="true" />
    </Variant>
    <Variant title="Error">
      <InputStateIcon :error="true" />
    </Variant>
    <Variant title="Disabled">
      <InputStateIcon :disabled="true" />
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import InputStateIcon from './InputStateIcon.vue';
</script>

<docs lang="md">
## Icons for input states

The `<InputStateIcon>` component displays the appropriate icon when an input
state changes (e.g., is invalid, etc.).

### Usage

The following attributes are available.

| Name     | Type    | Required | Description                                   | Default      |
| :------- | :------ | :------- | :-------------------------------------------- | :----------- |
| disabled | boolean | `false` | if true, the input will be disabled | `false` |
| valid | boolean | `false` | if true, the input will be styled as valid | `false` |
| error | string | `false` | display an error message if there is a form validation issue | --- |


### Examples

```js
<script setup>
import { ref } from "vue"; 
import { InputStateIcon, InputButton } from "molgenis-design-system";

const valid = ref(false);
const error = ref(false);
const disabled = ref(false);

</script>

<template>
  <div class="">
    <InputStateIcon
      :valid="valid"
      :error="error"
      :disabled="disabled"
    />
  </div>
  <div class="flex flex-row gap-3">
    <InputButton
      id="button-error"
      label="Show Error Icon"
      @click="() => error.value = true"
    />
    <InputButton
      id="button-valid"
      label="Show Valid Icon"
      @click="() => valid.value = true"
    />
    <InputButton
      id="button-disabled"
      label="Show Disabled Icon"
      @click="() => disabled.value = true"
    />
  </div>
</template>


```

</docs>