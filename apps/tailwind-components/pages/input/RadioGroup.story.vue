<template>
  <InputTestContainer show-state v-slot="{ invalid, valid, disabled }">
    <form>
      <fieldset>
        <legend id="radio-input-group-title" class="text-title">
          Select the patient's group allocation. If unknown, please leave blank.
        </legend>
        <InputRadioGroup
          id="radio-input-group"
          described-by="radio-input-group-title"
          v-model="selection"
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
          @focus="inputState = 'focused'"
          @blur="inputState = 'blurred'"
        />
      </fieldset>
    </form>
  </InputTestContainer>
  <div>
    <ul class="text-title">
      <li>Selection: {{ selection }}</li>
      <li>Input Group State: {{ inputState }}</li>
    </ul>
  </div>
</template>

<script lang="ts" setup>
const selection = ref<string>("intervention");
const inputState = ref<string>("inactive");
</script>
