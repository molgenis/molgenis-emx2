<script setup lang="ts">
const groupSelection = ref<string[]>(["tomatoes", "basil"]);
</script>

<template>
  <InputTestContainer show-state v-slot="{ invalid, valid, disabled }">
    <fieldset>
      <legend class="text-title">
        Which toppings would you like on your pizza? You may select more than
        one.
      </legend>
      <InputCheckboxGroup
        id="example-4"
        v-model="groupSelection"
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
      />
    </fieldset>
  </InputTestContainer>
  <output>
    <span>Selection: {{ groupSelection }}</span>
  </output>
</template>
