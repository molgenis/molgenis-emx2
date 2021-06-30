<template>
  <nav class="mg-page-nav" aria-label="breadcrumb">
    <ol class="breadcrumb">
      <li class="breadcrumb-item" v-for="(crumb, index) in tail" :key="index">
        <router-link v-if="crumb.to" router-link :to="{ path: crumb.to }">
          {{ crumb.label }}
        </router-link>
        <template v-else>{{ crumb.label }}</template>
      </li>
      <li class="breadcrumb-item active" aria-current="page">
        <router-link v-if="head.to" router-link :to="{ path: head.to }">
          {{ head.label }}
        </router-link>
        <template v-else>{{ head.label }}</template>
      </li>
    </ol>
  </nav>
</template>

<script>
export default {
  name: "BreadCrumbs",
  props: {
    /**
     * Ordered list of crumbs, each crumb should contain 'label' property to used in render
     * Optional 'to' property to be used as router-link to property (string | Location)
     *
     * If no crumbs are passed the default crumbs are created from the url
     */
    crumbs: Array,
  },
  computed: {
    head() {
      return this.crumbs && this.crumbs.length
        ? this.crumbs[this.crumbs.length - 1]
        : this.defaultCrumbs[this.defaultCrumbs.length - 1];
    },
    tail() {
      if (this.crumbs && this.crumbs.length) {
        return this.crumbs.length < 2
          ? []
          : this.crumbs.slice(0, this.crumbs.length - 1);
      } else {
        return this.defaultCrumbs.length < 2
          ? []
          : this.defaultCrumbs.slice(0, this.defaultCrumbs.length - 1);
      }
    },
    defaultCrumbs() {
      // todo add unit test to test url dancing
      let path = decodeURI(window.location.pathname).split("/");
      let url = "/";
      const defaultCrumbs = [{ label: "molgenis", to: url }];
      // todo central ??
      if (window.location.pathname != "/apps/central/") {
        path.forEach((pathComponent) => {
          if (pathComponent != "") {
            url += pathComponent + "/";
            defaultCrumbs.push({ label: pathComponent, to: url });
          }
        });
      }
      if (this.$route) {
        path = decodeURI(location.hash).substr(1).split("/");
        url += "#";
        path.forEach((el) => {
          if (el != "") {
            url += "/" + el;
            defaultCrumbs.push({ label: el, to: url });
          }
        });
      }
      console.log(defaultCrumbs);
      return defaultCrumbs;
    },
  },
};
</script>

<style>
/* remove default margins to get full width style within page */
nav.mg-page-nav {
  margin-top: -1rem;
  margin-left: -1rem;
  margin-right: -1rem;
}
</style>

<docs>
Single item example
```
const crumbs = [
  {label: 'item1'},
]
<BreadCrumbs :crumbs=crumbs />
```
Multi item example
```
const crumbs = [
  {label: 'item1'},
  {label: 'item2', to: 'as-a-string'},
  {label: 'item3', to: { name: 'user', params: { userId: 123 }}}
]
<BreadCrumbs :crumbs=crumbs />
```
Default crumbs example
```

<BreadCrumbs />
</docs>
