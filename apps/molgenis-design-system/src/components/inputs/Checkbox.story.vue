<template>
  <Story title="Inputs/Checkbox" :layout="{ type: 'grid', width: '400px' }">
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
  </Story>
</template>

<script setup lang="ts">
import Checkbox from "./Checkbox.vue";
</script>