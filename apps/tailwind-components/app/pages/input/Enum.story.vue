<template>
  <InputTestContainer
    show-state
    v-slot="{ invalid, valid, disabled, required }"
  >
    <form class="space-y-8" @submit.prevent>
      <div>
        <h3 class="text-heading-lg mb-2">ENUM: searchable listbox</h3>
        <fieldset>
          <legend id="enum-legend" class="text-title-contrast mb-1">
            Biological sex
          </legend>
          <InputEnum
            id="enum"
            v-model="scalarValue"
            :values="sexValues"
            :invalid="invalid"
            :valid="valid"
            :disabled="disabled"
            :required="required"
            described-by="enum-legend"
          />
        </fieldset>
        <p class="mt-2 text-title-contrast">
          Selected: {{ scalarValue ?? "(none)" }}
        </p>
      </div>

      <div>
        <h3 class="text-heading-lg mb-2">
          ENUM_ARRAY: searchable listbox with chips
        </h3>
        <fieldset>
          <legend id="enum-array-legend" class="text-title-contrast mb-1">
            Administrative tags
          </legend>
          <InputEnum
            id="enum-array"
            is-array
            v-model="arrayValue"
            :values="tagValues"
            :invalid="invalid"
            :valid="valid"
            :disabled="disabled"
            :required="required"
            described-by="enum-array-legend"
          />
        </fieldset>
        <p class="mt-2 text-title-contrast">
          Selected: {{ arrayValue ?? "(none)" }}
        </p>
      </div>

      <div>
        <h3 class="text-heading-lg mb-2">
          Pre-filled: editing an existing row
        </h3>
        <fieldset>
          <legend id="enum-prefilled-legend" class="text-title-contrast mb-1">
            Biological sex of a stored row
          </legend>
          <InputEnum
            id="enum-prefilled"
            v-model="prefilledValue"
            :values="sexValues"
            :invalid="invalid"
            :valid="valid"
            :disabled="disabled"
            :required="required"
            described-by="enum-prefilled-legend"
          />
        </fieldset>
        <p class="mt-2 text-title-contrast">
          Selected: {{ prefilledValue ?? "(none)" }}
        </p>
      </div>
    </form>
  </InputTestContainer>
</template>

<script setup lang="ts">
import { ref } from "vue";

const sexValues = ["male", "female", "unknown"];
const tagValues = ["research", "clinical", "rare", "archived"];

const scalarValue = ref<string | null>(null);
const arrayValue = ref<string[] | null>(null);
const prefilledValue = ref<string | null>("female");
</script>
