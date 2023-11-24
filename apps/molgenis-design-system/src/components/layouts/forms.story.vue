<template>
  <Story title="Layouts/Forms" :layout="{type: 'grid', width: '500px' }">
    <Variant title="Default">
      <Form
        id="default-form"
        title="My form title"
        description="A short description about the form"
      >
        <template v-slot:context>
          <div class="bg-blue-100">
            <p class="py-5 px-2 text-center">slot:context</p>
          </div>
        </template>
        <template v-slot:inputs>
          <div class="bg-gray-200">
            <p class="py-12 px-2 text-center">slot:inputs</p>
          </div>
        </template>
        <template v-slot:actions>
          <div class="bg-gray-100">
            <p class="py-5 px-2 text-center">slot:actions</p>
          </div>
        </template>
      </Form>
    </Variant>
    <Variant title="Full example">
      <Form
        id="patientSearch"
        title="Search for patients"
        description="Apply one or more of the following filters to create a subset of the registry."
      >
      <template v-slot:context>
        <p>There are over 50k+ patients in the registry. The data comes from organisations (e.g., universities, hospitals, medical practices, governments, etc.) in many countries. Data is submitted to the registry using structured forms and validated upon submission.</p>
      </template>
      <template v-slot:inputs>
        <InputText
          id="patient-search-all"
          type="search"
          label="Search all"
        />
        <CheckboxGroup
          id="countryInput"
          title="Select Countries"
          description="Data comes from many countries. If you would like to limit the results to one or more countries, then select them below."
          :data="options"
          row_id="rowId"
          row_label="country"
          row_value="code"
          row_checked="default"
        />
      </template>
      </Form>
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import Form from './forms.vue';
import InputText from '../inputs/InputText.vue';
import CheckboxGroup from '../inputs/CheckboxGroup.vue';

const options = [
  { rowId: 'AU', country: 'Australia', code: 'AU', default: true },
  { rowId: 'BE', country: 'Belgium', code: 'BE' },
  { rowId: 'CA', country: 'Canada', code: 'CA' },
  { rowId: 'MX', country: 'Mexico', code: 'MX' },
  { rowId: 'NL', country: 'Netherlands', code: 'NL', default: true },
  { rowId: 'UK', country: 'United Kingdom', code: 'UK' },
  { rowId: 'US', country: 'United States', code: 'US' },
]

</script>

<docs lang="md">
## Form Layouts

The `<Form>` component is a layout component that encapsulates one or more form input elements.....

### Import

```js
import { Form } from "molgenis-design-system";
```

### Usage

The form component takes the following arguments.

| Name | Type | Required | Description | Default
|:-----|:-----|:-------- |:------------|:-------
| id   | string | `true` | unique identifier for the form | ---
| title | string | `false` | a title that describes the purpose of the form | ---
| description | string | `false` | content that provides additional context to the form | ---
| formTitleHierarchy | string | `h3` | modify the heading level for the form from `h2:h6` | `h3`

#### Slots

In addition, there are two slots that are designed to separate form inputs from buttons. Use the `<template>` element and the following slot names to create the content in the form.

| Name | Vue Slot | Description
|:-----|:---------|:--------
| Context | `v-slot:context` | Additional text to be displayed after the title and the description
| Inputs | `v-slot:inputs` | Form content such as inputs, filters, etc.
| Actions | `v-slot:actions` | Form actions including "submit", "search", etc.

### Example

```js
<script setup lang="ts">
import { Form } from "molgenis-design-system";
</script>

<template>
  <Form 
    id="search"
    title="Search data"
    description="Search for data using one or more of the following filters. If no results are found, remove or adjust the search paramaters"
  >
    <template v-slot:context>
      ...
    </template>
    <template v-slot:inputs>
      ...
    </template>
    <template v-slot:buttons>
      ...
    </template>
  </Form>
</template>
```


</docs>