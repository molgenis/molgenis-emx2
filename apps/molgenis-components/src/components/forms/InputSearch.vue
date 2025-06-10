<template>
  <FormGroup
    :id="id"
    :label="label"
    :description="description"
    :errorMessage="errorMessage"
  >
    <InputGroup>
      <input
        :id="id"
        :ref="id"
        v-model="input"
        type="text"
        class="form-control"
        :aria-describedby="id"
        :placeholder
        @keyup="onKeyup"
      />
      <template v-slot:append>
        <button
          v-if="isClearBtnShown"
          @click="clearInput"
          class="btn btn-outline-primary"
          type="button"
        >
          <i class="fas fa-fw fa-times" />
        </button>
        <button @click="search" class="btn btn-outline-primary" type="button">
          <i class="fas fa-fw fa-search" />
        </button>
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script setup lang="ts">
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";
import { defineEmits, ref } from "vue";

const props = withDefaults(
  defineProps<{
    modelValue: string;
    isClearBtnShown?: boolean;
    isEmitOnType: boolean;
    id?: string;
    label?: string;
    description?: string;
    errorMessage?: string;
    placeholder?: string;
  }>(),
  { isClearBtnShown: false, isEmitOnType: false, placeholder: "Search" }
);

const input = ref<string>(props.modelValue || "");

const emit = defineEmits(["update:modelValue"]);

function onKeyup(event: KeyboardEvent) {
  if (event.key === "Enter" || props.isEmitOnType) {
    search();
  }
}

function search() {
  emit("update:modelValue", input.value);
}

function clearInput() {
  input.value = "";
  search();
}
</script>

<docs>
<template>
  <div>
    <label class="font-italic">Basic search field, which searches on button click or enter</label>
    <DemoItem>
      <InputSearch id="input-search-1" v-model="value1" />
      <div>You search: {{ value1 }}</div>
    </DemoItem>
    <label class="font-italic">Pre filled search value and clear button</label>
    <DemoItem>
      <InputSearch
        id="input-search-2"
        v-model="value2"
        :isClearBtnShown="true"
      />
      <div>You search: {{ value2 }}</div>
    </DemoItem>
  </div>
</template>
<script setup>
import { ref } from "vue";
const value1 = ref("");
const value2 = ref("apples");
</script>
</docs>
