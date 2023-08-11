<template>
  <div class="layout">
    <DisplayModule
      :admin="admin"
      :settings="settings?.one"
      @update="updateDisplayModule($event, 'one')"
    />
  </div>
</template>

<script setup>
import DisplayModule from "../Modules/DisplayModule.vue";

const props = defineProps({
  settings: Object,
  admin: Boolean,
});
const emit = defineEmits(["update"]);
function updateDisplayModule(value, id) {
  emit("update", { ...props.settings, [id]: value });
}
</script>

<style></style>
