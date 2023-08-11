<template>
  <div>
    <div v-if="admin">
      <input
        :value="settings.title"
        placeholder="title"
        @input="change($event, 'title')"
      /><br />
      <input
        :value="settings.text"
        placeholder="text"
        @input="change($event, 'text')"
      />
    </div>
    <h1>{{ settings.title }}</h1>
    <p>{{ settings.text }}</p>
  </div>
</template>

<script setup>
import { ref } from "vue";

const props = defineProps({
  settings: Object,
  admin: Boolean,
});

const emit = defineEmits(["update"]);

function change(event, id) {
  emit("update", { ...props.settings, [id]: event.target.value });
}
</script>

<style></style>
