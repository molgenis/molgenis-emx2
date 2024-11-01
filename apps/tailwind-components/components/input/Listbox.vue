<template>
  <ul class="mb-6">
    <li>selection: {{ modelValue }}</li>
    <li>hover: {{ hoveredOption }}</li>
    <li>focus: {{ focusedOption }}</li>
  </ul>

  <div class="w-full relative">
    <InputListboxToggle
      :id="`listbox-${id}-toggle`"
      :value="modelValue?.value"
      :label="modelValue?.label"
      :placeholder="placeholder"
      :controls="`listbox-${id}-options`"
      :required="required"
      :hasError="hasError"
      @click="openCloseListbox"
    />

    <InputListboxList
      :id="`listbox-${id}-options`"
      :expanded="listboxIsExpanded"
      :selected-elem-id="modelValue?.elemId"
    >
      <InputListboxListItem
        v-for="(option, index) in listboxOptions"
        :key="index"
        tabindex="0"
        :ref="listboxOptionRefs.set" 
        :listbox-id="`listbox-${id}-options`"
        :elem-id="`listbox-${id}-options-${index}`"
        :value="option.value"
        :label="option.label"
        :selected="option.value === modelValue?.value"
        @update:model-value="onSelection"
      />
    </InputListboxList>
  </div>
</template>

<script lang="ts" setup>
import { useTemplateRefsList } from "@vueuse/core";
import { ref } from "vue";

interface IListboxOption {
  elemId?: string;
  value: string | number | boolean;
  label?: string;
  selected?: boolean;
}

withDefaults(
  defineProps<{
    id: string;
    listboxLabelId: string;
    listboxOptions: IListboxOption[];
    required?: boolean;
    hasError?: boolean;
    placeholder?: string;
  }>(),
  {
    required: false,
    hasError: false,
    placeholder: "Select an option",
  }
);

const modelValue = defineModel<IListboxOption>();
const hoveredOption = ref<IListboxOption | null>();
const focusedOption = ref<IListboxOption | null>();
const listboxIsExpanded = ref<boolean>(false);

const listboxOptionRefs = useTemplateRefsList<HTMLLIElement>();

function openCloseListbox(value: boolean) {
  listboxIsExpanded.value = value;
  // if (listboxIsExpanded.value) {
  //   console.log(listboxOptionRefs.value[0]);
  // }
}

// onMounted(() => console.dir(listboxOptionRefs.value[0]))

function onSelection (data: IListboxOption) {
  modelValue.value = data;
}

</script>
