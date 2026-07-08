<template>
  <InputTestContainer show-state v-slot="{ invalid, valid, disabled }">
    <form class="space-y-6">
      <div>
        <h3 class="text-heading-lg mb-2">ENUM — single select</h3>
        <InputLabel for="enum-single" class="block mb-1 pl-0">
          Select a blood type
        </InputLabel>
        <InputEnum
          id="enum-single"
          v-model="singleValue"
          :values="bloodTypeValues"
          :invalid="invalid"
          :valid="valid"
          :disabled="disabled"
          placeholder="Select blood type"
        />
        <p class="mt-2 text-title-contrast">
          Selected: {{ singleValue ?? "(none)" }}
        </p>
      </div>

      <div>
        <h3 class="text-heading-lg mb-2">MODULE — single select</h3>
        <InputLabel for="module-single" class="block mb-1 pl-0">
          Select experiment type
        </InputLabel>
        <InputEnum
          id="module-single"
          v-model="moduleValue"
          :values="moduleValues"
          placeholder="Select module"
        />
        <p class="mt-2 text-title-contrast">
          Selected: {{ moduleValue ?? "(none)" }}
        </p>
      </div>

      <div>
        <h3 class="text-heading-lg mb-2">ENUM_ARRAY — multi select</h3>
        <fieldset>
          <legend id="enum-array-legend" class="text-title-contrast mb-1">
            Select active modules
          </legend>
          <InputEnumArray
            id="enum-array"
            v-model="multiValue"
            :values="moduleValues"
            :invalid="invalid"
            :valid="valid"
            :disabled="disabled"
            described-by="enum-array-legend"
          />
        </fieldset>
        <p class="mt-2 text-title-contrast">
          Selected: {{ multiValue ?? "(none)" }}
        </p>
      </div>
    </form>
  </InputTestContainer>
</template>

<script setup lang="ts">
import { ref } from "vue";

const bloodTypeValues = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];
const moduleValues = ["BodyMeasurements", "BloodPressure", "MRI", "Genetics"];

const singleValue = ref<string | null>(null);
const moduleValue = ref<string | null>(null);
const multiValue = ref<string[] | null>(null);
</script>
