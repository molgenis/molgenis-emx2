<template>
  <div class="edit-mode">
    <div style="display: flex; gap: 10px">
      <strong>{{ type }} Module </strong>
      <InputSelect
        id="EditBlock"
        style="margin: 0"
        :modelValue="type"
        :options="['Html', 'Header', 'Section', 'PieChart']"
        :required="true"
      />
      <Button type="primary" size="tiny"><BaseIcon name="ArrowUp"/></Button>
      <Button type="primary" size="tiny"><BaseIcon name="ArrowDown"/></Button>
      <Button type="primary" size="tiny"><BaseIcon name="Plus"/></Button>
      <Button type="primary" size="tiny"><BaseIcon name="Trash"/></Button>
    </div>
    <hr />
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
let props = defineProps<{
  type?: string;
}>();
</script>

<style lang="css">
.edit-mode {
  padding: 1em;
  background-color: rgb(255, 242, 188);
}
</style>
