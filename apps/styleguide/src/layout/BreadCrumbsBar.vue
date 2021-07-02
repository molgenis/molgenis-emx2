<template>
  <nav class="mg-page-nav" aria-label="breadcrumb">
    <ol class="breadcrumb">
      <li
        class="breadcrumb-item"
        :class="{ active: crumb.active }"
        v-for="(crumb, index) in crumbs"
        :key="index"
      >
        <router-link v-if="crumb.to" router-link :to="{ path: crumb.to }">
          {{ crumb.label }}
        </router-link>
        <template v-else>{{ crumb.label }}</template>
      </li>
    </ol>
  </nav>
</template>

<script>
export default {
  name: "BreadCrumbsBar",
  props: {
    /**
     * Ordered list of crumbs, each crumb should contain 'label' property to used in render
     * Optional 'to' property to be used as router-link to property (string | Location)
     * Optional 'active' property to toggle bootstrap active class (Boolean)
     */
    crumbs: Array,
  },
};
</script>

<docs>
Single item example
```
const crumbs = [
  {label: 'item1', active: true},
]
<BreadCrumbsBar :crumbs=crumbs />
```
Multi item example
```
const crumbs = [
  {label: 'item1', to: 'http://molgenis.org'},
  {label: 'item2', to: 'named mock route'},
  {label: 'item3', active: true}
]
<BreadCrumbsBar :crumbs=crumbs />
```

</docs>
