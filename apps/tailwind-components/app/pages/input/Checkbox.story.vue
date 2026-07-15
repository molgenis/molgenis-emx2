<template>
  <label class="text-title">Default checkbox</label>
  <div>
    <Checkbox :model-value="modelValue"></Checkbox>
  </div>
  <label class="text-title">Checked </label>
  <div>
    <Checkbox :model-value="true"></Checkbox>
  </div>

  <label class="text-title">Indeterminate </label>
  <div>
    <Checkbox :model-value="true" :indeterminate="true"></Checkbox>
  </div>

  <label class="text-title">
    Prevent Click / take over mode ( left = checked, middle = indeterminate,
    right = unchecked )
  </label>
  <div>
    <Checkbox :model-value="true" :prevent-click="true"></Checkbox>
    <Checkbox
      class="ml-4"
      :indeterminate="true"
      :prevent-click="true"
    ></Checkbox>
    <Checkbox
      class="ml-4"
      :model-value="false"
      :prevent-click="true"
    ></Checkbox>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import Checkbox from "../../components/input/Checkbox.vue";

const modelValue = ref(false);
</script>
