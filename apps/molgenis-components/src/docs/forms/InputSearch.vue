<template>
  <demo-item id="input-search" label="Search Input (extends BaseInput)">
    <div>
      <label class="font-italic">Basic search field ( with default 300ms debounce)</label>
      <InputSearch
        id="input-search-1"
        v-model="value1"
      />
      <div>You search: {{value1}}</div>
    </div>

    <div class="mt-3">
       <label class="font-italic">Pre filled search value</label>
      <InputSearch
        id="input-search-1"
        v-model="value2"
      />
      <div>You search: {{value2}}</div>
    </div>
  </demo-item>
</template>
<script>
export default {
    data: function () {
        return {
            value1: "",
            value2: "apples",
        };
    }
};
</script>