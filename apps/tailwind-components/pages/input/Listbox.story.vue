<template>
  <InputLabel id="pizza-toppings-label" class="block mb-3">
    <span>Which pizza topping is you favorite?</span>
  </InputLabel>
  <InputListbox
    id="pizza-toppings"
    listboxLabelId="pizza-toppings-label"
    :listboxOptions="[
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
  />
</template>

<script lang="ts" setup></script>
