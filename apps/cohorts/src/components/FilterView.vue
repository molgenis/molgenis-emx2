<template>
  <div>
    <TreeMultiFilter
      :columns="[
        {
          name: 'Collection Type',
          refTable: 'CollectionTypes',
          refColumn: 'name',
        },
        {
          name: 'Number Of Participants',
          refTable: 'AgeCategories',
          refColumn: 'name',
        },
        {
          name: 'Variable Topic',
          refTable: 'Topics',
          refColumn: 'name',
        },
        {
          name: 'Recruitment age',
          refTable: 'AgeCategories',
          refColumn: 'name',
        },
        {
          name: 'Inclusion Criteria',
          refTable: 'InclusionCriteria',
          refColumn: 'name',
        },
        {
          name: 'Population',
          refTable: 'InclusionCriteria',
          refColumn: 'name',
        },
        {
          name: 'Country',
          refTable: 'InclusionCriteria',
          refColumn: 'name',
        },
        {
          name: 'Host organisation',
          refTable: 'Organisation',
          refColumn: 'name',
        },
      ]"
    />
  </div>
</template>

<script>
import TreeMultiFilter from "./TreeMultiFilter";

export default {
  components: {
    TreeMultiFilter,
  },
};
</script>
