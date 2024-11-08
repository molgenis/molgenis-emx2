<template>
  <div class="w-full relative">
    <p>{{ focusCounter }}: {{ modelValue }}</p>
    <ul
      role="listbox"
      ref="listboxRef"
      class="bg-listbox"
      tabindex="0"
      @keydown.space.prevent
      @keydown.up.prevent="onKeyUp"
      @keydown.down.prevent="onKeyDown"
    >
      <li
        v-for="(option, index) in options"
        :id="`listbox-id-option-${index}`"
        ref="listboxOptionsRef"
        class="flex justify-start items-center gap-3 pl-3 py-1 text-listbox border-t-[1px] border-t-listbox-option hover:cursor-pointer hover:bg-listbox-hover hover:text-listbox focus:bg-listbox-selected focus:text-listbox-selected"
        :class="{
          '!bg-listbox-selected !text-listbox-selected':
            option.value === modelValue?.value,
        }"
        :aria-selected="option.value == modelValue?.value"
        @keydown.tab.prevent
        @click="(event: Event) => updateModelValue(event, option, index)"
        @focus="(event: Event) => updateModelValue(event, option, index)"
        @blur="(event: Event) => (event.target as HTMLOptionElement).setAttribute('tabindex', '-1')"
      >
        <BaseIcon
          name="Check"
          class="fill-listbox-selected"
          :class="option.value === modelValue?.value ? 'visible' : 'invisible'"
          :width="18"
        />
        <span>{{ option.value }}</span>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, useTemplateRef } from "vue";

interface IListboxOption {
  elemId?: string;
  value: string | number | boolean;
  label?: string;
  selected?: boolean;
  focused?: boolean;
}

const focusCounter = ref<number>(0);
const listboxRef = useTemplateRef<HTMLUListElement>("listboxRef");
const listboxOptionsRef =
  useTemplateRef<HTMLOptionElement[]>("listboxOptionsRef");
const modelValue = defineModel<IListboxOption>();
const options = ref<IListboxOption[]>([
  { value: "tomatoes", label: "Roma tomatoes" },
  { value: "pepperoni", label: "Pepperoni" },
  { value: "mozzerella", label: "Fresh mozzerella" },
  { value: "chillies", label: "Chillies" },
  { value: "basil", label: "Fresh basil" },
]);

function counterIsInRange(value: number) {
  return value <= options.value.length - 1 && value >= 0;
}

function updateCounter(value: number) {
  if (counterIsInRange(value)) {
    focusCounter.value = value;
  } else {
    focusCounter.value = focusCounter.value;
  }
}

function focusListOption() {
  nextTick(() => {
    if (listboxOptionsRef.value) {
      const targetElem = listboxOptionsRef.value[
        focusCounter.value
      ] as HTMLOptionElement;
      targetElem.setAttribute("tabindex", "0");
      targetElem.focus();
    }
  });
}

function updateModelValue(
  event: Event,
  selection: IListboxOption,
  index: number
) {
  const newSelection: IListboxOption = selection;
  const elemId: string = (event.target as HTMLOptionElement).id;

  modelValue.value = newSelection;
  modelValue.value.elemId = elemId;
  listboxRef.value?.setAttribute("aria-activedescendant", elemId);

  focusCounter.value = index;
}

function onKeyUp(event: Event) {
  console.log("key up");
  const newCounterValue = focusCounter.value - 1;
  updateCounter(newCounterValue);
  focusListOption();
}

function onKeyDown(event: Event) {
  console.log("key down");
  const newCounterValue = focusCounter.value + 1;
  updateCounter(newCounterValue);
  focusListOption();
}

onMounted(() => focusListOption());
</script>
