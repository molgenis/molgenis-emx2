<template>
  <li
    role="option"
    :aria-selected="selected"
    class="flex justify-start items-center gap-3 pl-3 py-1 text-listbox border-t-[1px] border-t-listbox-option cursor-pointer"
    :class="{
      'bg-listbox-selected text-listbox-selected': selected,
      'hover:bg-listbox-hover hover:text-listbox focus:bg-listbox-hover focus:text-listbox': !selected
    }"
    @click="emitSelection()"
    @focus="isFocused = true"
    @blur="isFocused = false"
    @keydown="onKeyboardEvent"
  >
    <BaseIcon
      name="Check"
      class="fill-listbox-selected"
      :class="selected ? 'visible' : 'invisible'"
      :width="18"
    />
    <span v-if="label">
      {{ label }}
    </span>
    <span v-else>
      {{ value }}
    </span>
  </li>
</template>

<script lang="ts" setup>
import { ref } from "vue";

interface IListBoxItemData {
  value: string | number | boolean;
  label?: string;
}

interface IListboxItem extends IListBoxItemData {
  elemId: string;
  selected?: boolean;
}

const props = withDefaults(defineProps<IListboxItem>(), {
  selected: false,
});

const modelValue = ref<IListboxItem>({
  elemId: props.elemId,
  value: props.value,
  label: props.label,
});

const isFocused = ref<boolean>(false);

const emit = defineEmits<{
  (e: "update:modelValue", option:IListBoxItemData): void;
  (e: "click", option: IListBoxItemData): void;
  (e: "focus", value: string): void;
}>();

function emitSelection() {
  emit("update:modelValue", modelValue.value);
}

function onKeyboardEvent (event: KeyboardEvent) {
  const key = event.key;
  if (key === 'ArrowDown') {
    event.preventDefault();
    console.log('down arrow pressed');
  } 
  if (key === 'ArrowUp') {
    event.preventDefault();
    console.log('up arrow pressed');
  }
  
  if (key === "Enter") {
    console.log("new selection")
    emitSelection();
  }
}

</script>
