<template>
  <div>
    <ul v-if="releases">
      <li v-for="r in releases" :key="r.resource.name + r.version">
        <RouterLink
          :to="{
            name: 'release',
            params: {
              pid: r.resource.pid,
              version: r.version,
            },
          }"
          >{{ r.resource.pid }} - version {{ r.version }}
        </RouterLink>
      </li>
    </ul>
    <p v-else>N/A</p>
  </div>
</template>

<script>
export default {
  props: {
    releases: Array,
  },
};
</script>
