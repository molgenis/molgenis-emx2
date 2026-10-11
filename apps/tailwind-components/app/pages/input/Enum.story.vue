<template>
  <InputTestContainer
    show-state
    v-slot="{ invalid, valid, disabled, required }"
  >
    <form class="space-y-8">
      <div>
        <h3 class="text-heading-lg mb-2">ENUM with 3 values: radio group</h3>
        <fieldset>
          <legend id="enum-short-legend" class="text-title-contrast mb-1">
            Biological sex
          </legend>
          <InputEnum
            id="enum-short"
            v-model="shortValue"
            :values="sexValues"
            :invalid="invalid"
            :valid="valid"
            :disabled="disabled"
            :required="required"
            described-by="enum-short-legend"
          />
        </fieldset>
        <p class="mt-2 text-title-contrast">
          Selected: {{ shortValue ?? "(none)" }}
        </p>
      </div>

      <div>
        <h3 class="text-heading-lg mb-2">
          ENUM with 21 values: searchable listbox
        </h3>
        <InputLabel for="enum-long" class="block mb-1 pl-0">
          Choose a code
        </InputLabel>
        <InputEnum
          id="enum-long"
          v-model="longValue"
          :values="codeValues"
          :invalid="invalid"
          :valid="valid"
          :disabled="disabled"
          :required="required"
          placeholder="Select a code"
        />
        <p class="mt-2 text-title-contrast">
          Selected: {{ longValue ?? "(none)" }}
        </p>
      </div>

      <div>
        <h3 class="text-heading-lg mb-2">ENUM_ARRAY: checkbox group</h3>
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
        <InputLabel for="enum-prefilled-long" class="block mt-4 mb-1 pl-0">
          Code of a stored row
        </InputLabel>
        <InputEnum
          id="enum-prefilled-long"
          v-model="prefilledLongValue"
          :values="codeValues"
          :invalid="invalid"
          :valid="valid"
          :disabled="disabled"
          :required="required"
        />
        <fieldset class="mt-4">
          <legend
            id="enum-prefilled-array-legend"
            class="text-title-contrast mb-1"
          >
            Tags of a stored row
          </legend>
          <InputEnum
            id="enum-prefilled-array"
            is-array
            v-model="prefilledArrayValue"
            :values="tagValues"
            :invalid="invalid"
            :valid="valid"
            :disabled="disabled"
            :required="required"
            described-by="enum-prefilled-array-legend"
          />
        </fieldset>
        <p class="mt-2 text-title-contrast">
          Selected: {{ prefilledValue ?? "(none)" }},
          {{ prefilledLongValue ?? "(none)" }},
          {{ prefilledArrayValue ?? "(none)" }}
        </p>
      </div>
    </form>
  </InputTestContainer>
</template>

<script setup lang="ts">
import { ref } from "vue";

const sexValues = ["male", "female", "unknown"];
const tagValues = ["research", "clinical", "rare", "archived"];
const codeValues = Array.from({ length: 21 }, (_, i) => `CODE-${i + 1}`);

const shortValue = ref<string | null>(null);
const longValue = ref<string | null>(null);
const arrayValue = ref<string[] | null>(null);
const prefilledValue = ref<string | null>("female");
const prefilledLongValue = ref<string | null>("CODE-17");
const prefilledArrayValue = ref<string[] | null>(["research", "rare"]);
</script>
