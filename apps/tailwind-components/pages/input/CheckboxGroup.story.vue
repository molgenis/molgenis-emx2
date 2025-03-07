<template>
  <InputTestContainer show-state v-slot="{ invalid, valid, disabled }">
    <form>
      <fieldset>
        <legend id="checkbox-input-group-title" class="text-title">
          Which toppings would you like on your pizza? You may select more than
          one.
        </legend>
        <InputCheckboxGroup
          id="checkbox-input-group"
          described-by="checkbox-input-group-title"
          v-model="selection"
          :valid="valid"
          :invalid="invalid"
          :disabled="disabled"
          :options="[
            {
              value: 'tomatoes',
              label: 'Roma tomatoes',
            },
            {
              value: 'pepperoni',
              label: 'Pepperoni',
            },
            {
              value: 'mozzerella',
              label: 'Fresh mozzerella',
            },
            {
              value: 'chillies',
              label: 'Chillies',
            },
            {
              value: 'basil',
              label: 'Fresh basil',
            },
          ]"
          :showClearButton="true"
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

<script setup lang="ts">
const selection = ref<string[]>(["tomatoes", "basil"]);
const inputState = ref<string>("inactive");
</script>
