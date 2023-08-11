<template>
  <div class="layout">
    <span v-if="admin" class="badge badge-pill badge-warning">
      Layout:
      <InputSelectInplace
        id="setLayout"
        label="Layout"
        required
        :modelValue="layout"
        @update:modelValue="updateLayout"
        :options="['SideFour', 'Four', 'One']"
    /></span>

    <component
      :is="GetLayout(settings?.layout || 'SideFour')"
      :settings="settings"
      :admin="admin"
      @update="updateComponent"
    />
  </div>
</template>

<script setup>
import { InputSelectInplace } from "molgenis-components";
import { ref } from "vue";
import One from "./One.vue";
import Four from "./Four.vue";
import SideFour from "./SideFour.vue";

const props = defineProps({
  settings: Object,
  admin: Boolean,
});

const emit = defineEmits(["update"]);

let layout = ref("One");

function updateLayout(value) {
  layout.value = value;
  emit("update", {
    ...props.settings,
    layout: value,
  });
}

function updateComponent(value) {
  console.log(props.settings);
  emit("update", value);
}

function GetLayout(layout) {
  return { One: One, Four: Four, SideFour: SideFour }[layout];
}
</script>

<style></style>
