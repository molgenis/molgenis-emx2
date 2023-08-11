<template>
  <div class="layout">
    <section style="grid-area: display1">
      <DisplayModule
        :admin="admin"
        :settings="settings?.one"
        @update="updateDisplayModule($event, 'one')"
      />
    </section>
    <section style="grid-area: display2">
      <DisplayModule
        :admin="admin"
        :settings="settings?.two"
        @update="updateDisplayModule($event, 'two')"
      />
    </section>
    <section style="grid-area: display3">
      <DisplayModule
        :admin="admin"
        :settings="settings?.three"
        @update="updateDisplayModule($event, 'three')"
      />
    </section>
    <section style="grid-area: display4">
      <DisplayModule
        :admin="admin"
        :settings="settings?.four"
        @update="updateDisplayModule($event, 'four')"
      />
    </section>
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

<style scoped>
.layout {
  width: 100%;
  height: 100%;
  display: grid;
  grid-template-columns: auto;
  grid-template-rows: auto;
  grid-template-areas:
    "display1 display2"
    "display3 display4";
}

.layout section {
  padding: 0.5rem;
  margin: 0.5rem;
  background-color: white;
  border-radius: 5px;
}
</style>
