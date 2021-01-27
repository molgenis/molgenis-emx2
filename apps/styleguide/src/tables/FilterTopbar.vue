<template>
  <div>
    <div :key="timestamp" class="d-flex flex-row flex-wrap">
      <FilterDropdown
        v-for="(column, idx) in filters"
        :title="column.name"
        :visible="column.showFilter"
        :key="column.name + column.updateTime"
        @remove="hideFilter(idx)"
        class="mr-2 mt-2"
      >
        <FilterInput v-model="filters[idx]" />
      </FilterDropdown>
    </div>
  </div>
</template>

<script>
import FilterSidebar from "./FilterSidebar";
import FilterDropdown from "./FilterDropdown";

export default {
  extends: FilterSidebar,
  components: { FilterDropdown },
};
</script>

<docs>
examples
```
<template>
  <div>
    <div class="row">
      <FilterWells v-model="filters"/>
      <FilterTopbar v-model="filters"/>
    </div>
  </div>


</template>

<script>
  export default {
    data: function () {
      return {
        filters: [{
          "name": "orderId",
          "pkey": true,
          "columnType": "STRING", showFilter: true
        },
          {
            "name": "category",
            "columnType": "REF",
            "refTable": "Topic", showFilter: true
          },
          {
            "name": "quantity",
            "columnType": "INT", showFilter: true
          },
          {
            "name": "price",
            "columnType": "DECIMAL", showFilter: true
          },
          {
            "name": "complete",
            "columnType": "BOOL", showFilter: true
          },
          {
            "name": "status",
            "columnType": "STRING", showFilter: true
          },
          {
            "name": "birthday",
            "columnType": "DATE", showFilter: true
          }]
      }
    }
  }
</script>
```

</docs>
