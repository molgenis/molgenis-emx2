<template>
  <demo-item id="filter-well" label="Filter well">
    <FilterWell label="foo = bar" @click="showAlert"/>
  </demo-item>
</template>
<script>
export default {
    data: function () {
        return {
        };
    },
    methods: {
        showAlert () {
            alert('clicked')
        }
    }
};
</script>