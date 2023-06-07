<template>
  <ul v-if="networks">
    <li v-for="d in networks" :key="d.id">
      <RouterLink
        :to="{
          name: 'network',
          params: { id: d.id },
        }">
        {{ d.id }} - {{ d.name }}
      </RouterLink>
    </li>
  </ul>
  <p v-else>N/A</p>
</template>
<script>
export default {
  props: { networks: Array },
};
</script>
