<template>
  <div class="edit-mode">
    <div style="display: flex; gap: 10px">
      <strong>{{ type }} Module </strong>
      <InputSelect
        id="EditBlock"
        style="margin: 0"
        :modelValue="type"
        :options="['Html', 'Header', 'Section', 'PieChart']"
      />
      <ButtonAction> move up</ButtonAction>
      <ButtonAction> move down</ButtonAction>
      <ButtonAction> add new module below</ButtonAction>
      <ButtonDanger icon="times" style="margin-left: auto">
        remove</ButtonDanger
      >
    </div>
    <hr />
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
import { InputSelect, ButtonDanger, ButtonAction } from "molgenis-components";
let props = defineProps<{
  type?: string;
}>();
</script>

<style lang="css">
.edit-mode {
  padding: 1em;
  background-color: rgb(255, 242, 188);
}
</style>
