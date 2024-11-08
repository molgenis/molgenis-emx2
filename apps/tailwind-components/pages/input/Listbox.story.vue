<template>
  <InputLabel id="listbox-input-label" class="block mb-3">
    <span>Select a group assignment</span>
  </InputLabel>
  <InputListbox
    id="listbox-input"
    labelId="listbox-input-label"
    :options="data"
  />
</template>

<script lang="ts" setup>
const data = [...Array(26).keys()].map((num: number) => {
  return {
    value: num,
    label: "Group " + String.fromCharCode(num + 65),
  };
});
</script>
