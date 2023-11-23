<template>
  <Story title="Inputs/Checkboxes" :layout="{ type: 'grid', width: '400px' }">
    <Variant title="Single Checkbox">
      <Checkbox
        id="input-1"
        label="Do you agree to the terms and conditions?"
      />
    </Variant>
    <Variant title="Single Checkbox with description">
      <Checkbox
        id="input-2"
        label="Do you agree to the terms and conditions?"
        description="Ipsum nisi culpa adipisicing ut. Aliquip eu laboris anim adipisicing pariatur qui mollit aliqua mollit proident minim. Voluptate culpa anim ullamco ullamco exercitation culpa. Ex pariatur laborum et velit consectetur deserunt ex nisi incididunt occaecat excepteur nisi amet. Velit ad eu enim dolor."
      />
    </Variant>
    <Variant title="Checkbox Group">
      <CheckboxGroup
        id="groupInput1"
        name="pizza-toppings"
        title="Customise your pizza"
        description="All pizzas are made with flour from the local mill and covered with our freshly made tomato sauce."
        :data="[
          {rowId: 'T001', topping: 'Mozzarella', value: 'mozzarella', desc: 'Slices of our fresh mozzerella'},
          {rowId: 'T002', topping: 'Tomatoes', value: 'tomatoes', desc: 'Perfectly ripe cherry tomatoes from our garden'},
          {rowId: 'T003', topping: 'Basil', value: 'basil', desc: 'Fresh basil grown from our garden'},
          {rowId: 'T004', topping: 'Chilies', value: 'chilies', desc: 'Spice up your pizza with Tepin Chilies'},
        ]"
        data_id="rowId"
        data_label="topping"
        data_value="value"
        data_description="desc"
      />
    </Variant>
  </Story>
</template>

<script setup lang="ts">
import Checkbox from "./Checkbox.vue";
import CheckboxGroup from "./CheckboxGroup.vue";
</script>