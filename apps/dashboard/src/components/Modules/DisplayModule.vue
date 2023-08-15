<template>
  <div>
    <span v-if="admin" class="badge badge-pill badge-warning">
      Component:
      <InputSelectInplace
        id="setLayout"
        label="Layout"
        required
        :modelValue="module"
        @update:modelValue="updateDisplay"
        :options="['Empty', 'Text', 'Graph', 'Table', 'Aggregate']"
    /></span>

    <component
      :is="GetDisplay(settings?.module || 'Empty')"
      :settings="settings"
      :admin="admin"
      @update="updateComponent"
    />
  </div>
</template>

<script setup>
import { ref } from "vue";

import { InputSelectInplace } from "molgenis-components";

import EmptyDisplay from "./EmptyDisplay.vue";
import TextDisplay from "./TextDisplay.vue";
import GraphDisplay from "./GraphDisplay.vue";
import AggregateDisplay from "./AggregateDisplay.vue";
import TableDisplay from "./TableDisplay.vue";

const props = defineProps({
  settings: Object,
  admin: Boolean,
});

const emit = defineEmits(["update"]);

const module = ref("Empty");

function updateDisplay(value) {
  module.value = value;
  emit("update", {
    ...props.settings,
    module: value,
  });
}

function updateComponent(value) {
  emit("update", value);
}
function GetDisplay(display) {
  return {
    Empty: EmptyDisplay,
    Text: TextDisplay,
    Graph: GraphDisplay,
    Aggregate: AggregateDisplay,
    Table: TableDisplay,
  }[display];
}
</script>

<style></style>
