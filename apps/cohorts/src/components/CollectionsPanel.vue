<template>
  <div>
    {{ collection.name }}
    <ul>
      <li v-for="table in collection.tables">
        {{ table.name }}
        <ul>
          <li v-for="variable in table.variables">
            {{ variable.name }}
          </li>
        </ul>
      </li>
    </ul>
  </div>
</template>

<script>
export default {
  props: {
    collection: Object,
  },
};
</script>
