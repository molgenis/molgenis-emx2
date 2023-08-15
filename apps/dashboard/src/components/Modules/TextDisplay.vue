<template>
  <div>
    <div v-if="admin">
      <InputString
        id="TextDisplayTitle"
        :modelValue="title"
        placeholder="title"
        @update:modelValue="change($event, 'title')"
        class="mb-1"
      />
      <InputString
        id="TextDisplayText"
        :modelValue="text"
        placeholder="text"
        @update:modelValue="change($event, 'text')"
      />
    </div>
    <h1>{{ settings.title }}</h1>
    <p>{{ settings.text }}</p>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { InputString } from "molgenis-components";

const props = defineProps({
  settings: Object,
  admin: Boolean,
});

let title = props.setting?.title;
let text = props.setting?.text;

watch(props, (newProps) => {
  if (title === "" && newProps.settings.title !== "")
    title = newProps.settings.title;
  if (text === "" && newProps.settings.text !== "")
    text = newProps.settings.text;
});

const emit = defineEmits(["update"]);

function change(value, id) {
  emit("update", { ...props.settings, [id]: value });
}
</script>

<style></style>
