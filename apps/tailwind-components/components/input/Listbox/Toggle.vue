<template>
  <button
    :id="id"
    :aria-controls="controls"
    :aria-expanded="expanded"
    :aria-required="required"
    class="flex justify-start items-center h-10 w-full text-left pl-10 border-input bg-input text-button-input-toggle"
    @click="onClick"
  >
    <span class="w-full" v-if="label">
      {{ label }}
    </span>
    <span class="w-full" v-else-if="value">
      {{ value }}
    </span>
    <span class="w-full" v-else>
      {{ placeholder }}
    </span>
    <div class="w-[60px] flex flex-col">
      <BaseIcon :width="18" name="caret-up" class="mx-auto -my-1" />
      <BaseIcon :width="18" name="caret-down" class="mx-auto -my-1" />
    </div>
  </button>
</template>

<script lang="ts" setup>
import { ref } from "vue";

interface IInputToggle {
  id: string;
  value?: string | number | boolean;
  label?: string;
  placeholder?: string;
  controls: string;
  required: boolean;
  hasError: boolean;
}

withDefaults(defineProps<IInputToggle>(), {
  placeholder: "Select an option",
});

const expanded = ref<boolean>(false);

const emit = defineEmits<{
  (e: "click", value: boolean): void;
}>();

function onClick () {
  expanded.value = !expanded.value;
  emit("click", expanded.value);
}

</script>
