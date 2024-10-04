<script setup lang="ts">
const defaultSelection = ref<boolean>(false);
const valueSelection = ref<string>("No");
const requiredSelection = ref<string>("No");
</script>

<template>
  <h2>Checkbox component</h2>
  <div
    class="grid grid-cols-2 gap-6 my-5 [&>div]:p-1 [&_h3]:font-semibold [&_h3]:my-2"
  >
    <div>
      <h3>Default checkbox</h3>
      <InputCheckbox
        id="checkbox-default"
        v-model="defaultSelection"
        value="test"
        checked="true"
      />
      <InputLabel for="checkbox-default">
        Do you aggree to the terms and conditions?
      </InputLabel>
      <p>Answer: {{ defaultSelection }}</p>
    </div>
    <div>
      <h3>Checkbox with yes and no values defined</h3>
      <InputCheckbox
        id="checkbox-default-labels"
        v-model="valueSelection"
        true-value="Yes"
        false-value="No"
        checked="true"
      />
      <InputLabel for="checkbox-default-labels">
        Do you aggree to the terms and conditions?
      </InputLabel>
      <p>Answer: {{ valueSelection }}</p>
    </div>
    <div>
      <h3>Checkbox with yes and no values defined and as a required input</h3>
      <InputCheckbox
        id="checkbox-default-labels-required"
        v-model="requiredSelection"
        true-value="Yes"
        false-value="No"
        checked="true"
        required="true"
      />
      <InputLabel for="checkbox-default-labels-required" :required="true">
        Do you aggree to the terms and conditions?
      </InputLabel>
      <p>Answer: {{ requiredSelection }}</p>
    </div>
  </div>
  <form>
    <legend class="text-title">
      Which features would you like to make available for end users?
    </legend>
    <InputCheckboxGroup
      id="example-4"
      :checkbox-options="[
        {
          value: 'database',
          label: 'Building databases from templates or importing your own',
        },
        { value: 'scripts', label: 'Writing and executing scripts' },
        { value: 'apps', label: 'Designing and developing custom interfaces' },
        { value: 'users', label: 'Managing user accounts and access levels' },
      ]"
    />
  </form>
</template>
