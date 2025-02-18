<template>
  <InputTestContainer show-state v-slot="{ invalid, valid, disabled }">
    <form>
      <fieldset>
        <legend class="text-title">
          Select the patient's group allocation. If unknown, please leave blank.
        </legend>
        <InputRadioGroup
          id="radio-input-group"
          v-model="example"
          :options="[
            { value: 'control', label: 'Healthy control' },
            {
              value: 'intervention',
              label: 'Experimental cohort',
            },
            { value: 'placebo', label: 'Placebo cohort' },
          ]"
          :show-clear-button="true"
          :invalid="invalid"
          :valid="valid"
          :disabled="disabled"
        />
      </fieldset>
    </form>
    <output>
      <span>Selection: {{ example }}</span>
    </output>
  </InputTestContainer>
</template>

<script lang="ts" setup>
const example = ref<string>("intervention");
</script>
