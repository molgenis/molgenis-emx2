<script setup lang="ts">
import { useId } from "vue";

const props = withDefaults(
  defineProps<{
    modelValue: string;
    placeholder?: string;
    inverted?: boolean;
    label?: string;
  }>(),
  {
    placeholder: "Type to search..",
    inverted: false,
    label: "Search",
  }
);

const emit = defineEmits(["update:modelValue"]);

function submitSearch() {
  emit("update:modelValue", props.modelValue);
}

let timeoutID: number | NodeJS.Timeout | undefined = undefined;
function handleInput(input: string) {
  clearTimeout(timeoutID);
  timeoutID = setTimeout(() => {
    emit("update:modelValue", input);
  }, 500);
}

const inputId = useId();
</script>
<template>
  <form class="relative" @submit.prevent="submitSearch()">
    <label :for="`search-input-${inputId}`" class="sr-only">{{ label }}</label>
    <input
      :id="`search-input-${inputId}`"
      type="search"
      :value="modelValue"
      @input="(event) => handleInput((event.target as HTMLInputElement).value)"
      class="w-full pr-16 font-sans text-black text-gray-300 bg-white outline-none rounded-theme h-10 ring-red-500 pl-3 shadow-search-input focus:shadow-search-input hover:shadow-search-input"
      :class="[
        inverted
          ? 'border-search-input-mobile border'
          : 'border-search-input focus:border-white',
      ]"
      :placeholder="placeholder"
    />
    <button
      class="rounded-theme absolute top-0 right-0 flex items-center pl-8 pr-6 tracking-wider text-search-button border-search-button border-[1px] transition-colors bg-search-button h-10 font-display text-heading-xl hover:text-search-button-hover"
    >
      Search
    </button>
  </form>
</template>
