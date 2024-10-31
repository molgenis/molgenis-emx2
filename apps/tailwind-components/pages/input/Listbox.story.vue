<template>
  <InputListbox
    id="pizzaToppings"
    listboxLabel="Which pizza topping is you favorite?"
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
