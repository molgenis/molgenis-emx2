<template>
  <Story
    title="Inputs/Checkbox Group"
    :layout="{ type: 'grid', width: '400px' }"
  >
    <Variant title="Checkbox Group">
      <CheckboxGroup
        id="groupInput1"
        title="Customise your pizza"
        description="All pizzas are made with flour from the local mill and covered with our freshly made tomato sauce."
        :data="data"
        row_id="rowId"
        row_label="topping"
        row_value="value"
      />
    </Variant>
    <Variant title="Checkbox Group with descriptions and defaults">
      <CheckboxGroup
        id="groupInput2"
        title="Customise your pizza"
        description="All pizzas are made with flour from the local mill and covered with our freshly made tomato sauce."
        :data="data"
        row_id="rowId"
        row_label="topping"
        row_value="value"
        row_description="desc"
        row_checked="default"
      />
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import CheckboxGroup from "./CheckboxGroup.vue";

const data = [
  {
    rowId: "T001",
    topping: "Mozzarella",
    value: "mozzarella",
    desc: "Slices of our fresh mozzerella",
    default: true,
  },
  {
    rowId: "T002",
    topping: "Tomatoes",
    value: "tomatoes",
    desc: "Perfectly ripe cherry tomatoes from our garden",
    default: false,
  },
  {
    rowId: "T003",
    topping: "Basil",
    value: "basil",
    desc: "Fresh basil grown from our garden",
    default: true,
  },
  {
    rowId: "T004",
    topping: "Chilies",
    value: "chilies",
    desc: "Spice up your pizza with Tepin Chilies",
    default: true,
  },
];
</script>
