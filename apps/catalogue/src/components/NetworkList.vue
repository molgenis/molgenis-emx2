<template>
  <ul v-if="networks">
    <li v-for="d in networks" :key="d.pid">
      <RouterLink
        :to="{
          name: 'network',
          params: { pid: d.pid },
        }"
      >
        {{ d.pid }} - {{ d.name }}
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
