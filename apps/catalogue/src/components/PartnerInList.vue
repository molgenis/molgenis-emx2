<template>
  <ul v-if="partnerIn">
    <li v-for="p in partnerIn">
      <RouterLink
        :to="{
          name: route(p.resource.mg_tableclass),
          params: { pid: p.resource.pid },
        }"
      >
        {{ p.resource.pid }} - {{ p.resource.name }} ({{ p.role.name }})
      </RouterLink>
    </li>
  </ul>
  <p v-else>N/A</p>
</template>

<script>
export default {
  props: {
    partnerIn: Array,
  },
  methods: {
    route(tableName) {
      return tableName.split(".")[1].slice(0, -1).toLowerCase();
    },
  },
};
</script>
