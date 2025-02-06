<template>
  <div class="border-t-2 border-dashed bg-yellow-200">
    <div v-if="!show">
      <div class="float-right -mt-12 flex">
        <Button  type="primary" size="small" class="mr-2" @click="show = true" label="Edit" />
        <ButtonBar class="mr-4">
          <Button type="primary" size="small" @click="$emit('action', 'up')" iconOnly icon="caret-up" label="Move up" />
          <Button type="primary" size="small" @click="$emit('action', 'down')" iconOnly icon="caret-down" label="Move down" />
          <Button type="primary" size="small" @click="$emit('action', 'add')" iconOnly icon="plus" label="Add above" />
          <Button type="primary" size="small" @click="$emit('action', 'delete')" iconOnly  icon="trash" label="Remove" />
        </ButtonBar>
      </div>
    </div>

    <div v-else class="border-b-2 border-dashed  p-3">
      <div class="mb-3 flex gap-[0.5rem]">
        <InputSelect
          id="EditBlock"
          style="margin: 0"
          :modelValue="type"
          :options="['Html', 'Header', 'Section', 'PieChart']"
          :required="true"
        />
        <Button type="primary" size="small" @click="show = false" label="hide" />
        <ButtonBar class="">
          <Button type="primary" size="small" @click="$emit('action', 'up')" iconOnly icon="caret-up" label="Move up" />
          <Button type="primary" size="small" @click="$emit('action', 'down')" iconOnly icon="caret-down" label="Move down" />
          <Button type="primary" size="small" @click="$emit('action', 'add')" iconOnly icon="plus" label="Add" />
          <Button type="primary" size="small" @click="$emit('action', 'delete')" iconOnly  icon="trash" label="Remove" />
        </ButtonBar>

      </div>
      <hr />
    <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
let props = defineProps<{
  type?: string;
}>();
defineEmits(['action']);
let show = ref(false);
</script>
