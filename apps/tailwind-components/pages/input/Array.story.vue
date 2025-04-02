<template>
  <InputTestContainer
    show-placeholder
    show-state
    v-slot="{ placeholder, valid, invalid, disabled }"
  >
    <div class="my-4">String array</div>
    <InputArray
      id="input-array-string"
      type="STRING_ARRAY"
      v-model="stringValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    <div class="my-4">Bool array</div>
    <InputArray
      id="input-array-bool"
      type="BOOL_ARRAY"
      v-model="boolValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    <div class="my-4">Datetime array</div>
    <InputArray
      id="input-array-datetime"
      type="DATETIME_ARRAY"
      v-model="timeValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    <div class="my-4">Decimal array</div>
    <InputArray
      id="input-array-decimal"
      type="DECIMAL_ARRAY"
      v-model="numberValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    <div class="my-4">Period array</div>
    <InputArray
      id="input-array-period"
      type="PERIOD_ARRAY"
      v-model="stringValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    <div class="my-4">Email array</div>
    <InputArray
      id="input-array-emai"
      type="EMAIL_ARRAY"
      v-model="stringValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    <div class="my-4">Hyperlink array</div>
    <InputArray
      id="input-array-link"
      type="HYPERLINK_ARRAY"
      v-model="stringValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    <div class="my-4">Int array</div>
    <InputArray
      id="input-array-int"
      type="INT_ARRAY"
      v-model="numberValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    <div class="my-4">Long array</div>
    <InputArray
      id="input-array-long"
      type="LONG_ARRAY"
      v-model="numberValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    <div class="my-4">Text array</div>
    <InputArray
      id="input-array-text"
      type="TEXT_ARRAY"
      v-model="stringValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
    <div class="my-4">UUID array</div>
    <InputArray
      id="input-array-uuid"
      type="UUID_ARRAY"
      v-model="stringValue"
      :placeholder="placeholder"
      :valid="valid"
      :invalid="invalid"
      :disabled="disabled"
    />
  </InputTestContainer>
</template>

<script setup lang="ts">
import { ref } from "vue";

const stringValue = ref(["string 1", "string 2"]);
const boolValue = ref([true, false]);
const timeValue = ref([Date.now(), Date.now()]);
const numberValue = ref([1, 2]);
</script>
