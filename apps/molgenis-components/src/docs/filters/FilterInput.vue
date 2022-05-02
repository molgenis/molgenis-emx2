<template>
  <demo-item id="filter-input" label="Filter input">
    <div>
      <label>emtpy string filter</label>
      <div>
        <FilterInput
          id="emtpy-string-filter"
          :conditions="conditions"
          @updateConditions="conditions = $event"
        ></FilterInput>
      </div>
      <div>conditions: {{ conditions }}</div>
    </div>

    <div class="mt-3">
      <label>pre-filled string filter</label>
      <div>
        <FilterInput
          id="pre-filled-string-filter"
          :conditions="conditions1"
          @updateConditions="conditions1 = $event"
        ></FilterInput>
      </div>
      <div>conditions: {{ conditions1 }}</div>
    </div>
  </demo-item>
</template>
<script>
export default {
  data: function () {
    return {
      conditions: [],
      conditions1: ["tst"],
    };
  },
};
</script>