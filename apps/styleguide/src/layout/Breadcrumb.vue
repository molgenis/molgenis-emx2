<template>
  <nav aria-label="breadcrumb">
    <ol class="breadcrumb">
      <li
        v-for="(url, label) in crumbs"
        :key="label"
        class="breadcrumb-item"
        :class="{ active: label === lastKey }"
      >
        <a v-if="label == lastKey" aria-current="page">{{ label }}</a>
        <a v-else :href="url">{{ label }}</a>
      </li>
    </ol>
  </nav>
</template>

<script>
export default {
  props: {
    /* pass Object {'label':'url'} pairs*/
    crumbs: Object,
  },
  computed: {
    lastKey() {
      return Object.keys(this.crumbs)[Object.keys(this.crumbs).length - 1];
    },
  },
};
</script>

<docs>
Example:
```
<Breadcrumb :crumbs="{'Home':'/', 'Schema':'/schema', 'Page':'/schema/page'}"/>
```
</docs>
