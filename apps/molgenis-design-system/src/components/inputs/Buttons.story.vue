<template>
  <Story title="Inputs/Buttons" :layout="{ type: 'grid', width: '75%' }">
    <Variant title="Button Primary">
      <div class="[&_button]:mb-4">
        <InputButton label="xs" type="submit" context="primary" size="xs"/>
        <InputButton label="sm" type="submit" context="primary" size="sm"/>
        <InputButton label="base" type="submit" context="primary" size="base"/>
        <InputButton label="lg" type="submit" context="primary" size="lg"/>
        <InputButton label="xl" type="submit" context="primary" size="xl"/>
        <InputButton label="2xl" type="submit" context="primary" size="2xl" />
        <InputButton label="3xl" type="submit" context="primary" size="3xl" />
      </div>
    </Variant>
    <Variant title="Button Secondary">
      <div class="[&_button]:mb-4">
        <InputButton label="xs" type="submit" context="secondary" size="xs"/>
        <InputButton label="sm" type="submit" context="secondary" size="sm"/>
        <InputButton label="base" type="submit" context="secondary" size="base"/>
        <InputButton label="lg" type="submit" context="secondary" size="lg"/>
        <InputButton label="xl" type="submit" context="secondary" size="xl"/>
        <InputButton label="2xl" type="submit" context="secondary" size="2xl"/>
        <InputButton label="3xl" type="submit" context="secondary" size="3xl"/>
      </div>
    </Variant>
    <Variant title="Button Tertiary">
      <div class="[&_button]:mb-4">
        <InputButton label="xs" type="submit" context="tertiary" size="xm"/>
        <InputButton label="sm" type="submit" context="tertiary" size="sm"/>
        <InputButton label="base" type="submit" context="tertiary" size="base"/>
        <InputButton label="lg" type="submit" context="tertiary" size="lg"/>
        <InputButton label="xl" type="submit" context="tertiary" size="xl"/>
        <InputButton label="2xl" type="submit" context="tertiary" size="2xl"/>
        <InputButton label="3xl" type="submit" context="tertiary" size="3xl"/>
      </div>
    </Variant>
    <Variant title="Button Outlined">
      <div class="[&_button]:mb-4">
        <InputButton label="xs" type="submit" context="outline" size="xs"/>
        <InputButton label="sm" type="submit" context="outline" size="sm"/>
        <InputButton label="base" type="submit" context="outline" size="base"/>
        <InputButton label="lg" type="submit" context="outline" size="lg"/>
        <InputButton label="xl" type="submit" context="outline" size="xl"/>
        <InputButton label="2xl" type="submit" context="outline" size="2xl"/>
        <InputButton label="3xl" type="submit" context="outline" size="3xl"/> 
      </div>
    </Variant>
    <Variant title="Disabled Button">
      <div class="[&_button]:mb-4">
        <InputButton label="xs" type="submit" size="xs" :is-disabled="true" />
        <InputButton label="sm" type="submit" size="sm" :is-disabled="true" />
        <InputButton label="base" type="submit" size="base" :is-disabled="true" />
        <InputButton label="lg" type="submit" size="lg" :is-disabled="true" />
        <InputButton label="xl" type="submit" size="xl" :is-disabled="true" />
        <InputButton label="2xl" type="submit" size="2xl" :is-disabled="true" />
        <InputButton label="3xl" type="submit" size="3xl" :is-disabled="true" />
      </div>
    </Variant>
  </Story>
</template>

<script setup>
import InputButton from "./Buttons.vue";
</script>

<docs lang="md">
## Input Buttons

The `<InputButton>` component is designed to be used when performing an action on a web page (e.g., form submission, file upload, etc.).

### Usage

The button component takes the following arguments.

| Name | Type | Required | Description | Default
|:-----|:-----|:-------- |:------------|:-------
| label | string | `true` | text that describes the button's purpose | ---
| type | string | `true` | specify the button type (`'button' \| 'reset' \| 'submit'`) | `default`
| context | string | `false` | set the hierarchy of the button (`'primary' \| 'secondary' \| 'outline' \| 'tertiary'`) | `secondary`

#### Note

This component should not be used for hyperlinks. Instead, use the `<a>` element and apply the approprate styling. For example:

```html
<a class="button-primary text-button-primary..."></a>
```

#### Slots

Use the `<template>` element and the following slot names to create the content in the form.


| Name | Vue Slot | Description
|:-----|:---------|:--------
| Icon | `v-slot:icon` | add an icon to a button

### Example

```js
<script setup lang="ts">
import { InputButton } from "molgenis-design-system";
</script>

<template>
  <InputButton label="Submit" type="submit" context="primary">
    <template v-slot:icon>
      <svg>...</svg>
    </template>
  </InputButton>
</template>



</docs>