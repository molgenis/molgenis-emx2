<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    id: string;
    modelValue?: string;
    placeholder?: string;
    inverted?: boolean;
  }>(),
  {
    placeholder: "Type to search..",
    inverted: false,
    modelValue: "",
  }
);

const emit = defineEmits(["update:modelValue"]);

let timeoutID: number | NodeJS.Timeout | undefined = undefined;
function handleInput(input: string) {
  clearTimeout(timeoutID);
  timeoutID = setTimeout(() => {
    emit("update:modelValue", input);
  }, 500);
}
</script>
<template>
  <div class="relative">
    <div
      class="absolute inset-y-0 start-0 flex items-center ps-3 pointer-events-none"
    >
      <BaseIcon name="search" />
    </div>
    <input
      :id="id"
      type="search"
      :value="modelValue"
      @input="(event) => handleInput((event.target as HTMLInputElement).value)"
      class="w-full pr-4 font-sans text-black text-gray-300 bg-white outline-none rounded-search-input h-10 ring-red-500 pl-10 shadow-search-input focus:shadow-search-input hover:shadow-search-input"
      :class="[
        inverted
          ? 'border-search-input-mobile border'
          : 'border-search-input focus:border-white',
      ]"
      :placeholder="placeholder"
    />
  </div>
</template>
