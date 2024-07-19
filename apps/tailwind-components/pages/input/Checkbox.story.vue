<script setup lang="ts">
const defaultSelection = ref<boolean>(false);
const valueSelection = ref<string>("No");
</script>

<template>
  <h2>Checkbox component</h2>
  <div class="grid grid-cols-2 gap-6 my-5 [&>div]:p-4 [&_h3]:font-semibold [&_h3]:my-2">
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
  </div>
</template>
