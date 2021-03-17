<template>
    <div>
        <div class="d-flex flex-row flex-wrap">
            <FilterDropdown
                v-for="(column, idx) in filters"
                :key="column.name + column.updateTime"
                class="mr-2 mt-2"
                :title="column.name"
                :visible="column.showFilter"
                @remove="hideFilter(idx)"
            >
                <FilterInput v-model="filters[idx]" />
            </FilterDropdown>
        </div>
    </div>
</template>

<script>
import FilterSidebar from "./FilterSidebar.vue";
import FilterDropdown from "./FilterDropdown.vue";

export default {
  components: { FilterDropdown },
  extends: FilterSidebar,
};
</script>
