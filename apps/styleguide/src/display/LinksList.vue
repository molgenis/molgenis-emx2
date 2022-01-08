<template>
  <div>
    <span class="mr-3" v-for="(item, index) in items" :key="index">
      <span class="font-weight-bold pr-1">{{ item.label }}</span>
      <span v-show="item.text" class="">{{ item.text }}</span>
      <a :href="item.href">{{ item.href }}</a>
    </span>
  </div>
</template>

<style></style>

<script>
export default {
  name: "LinksList",
  props: {
    /**
     * List of link items
     * Link item has:
     * label: String (required)
     * href: String (required)
     * text: String
     */
    items: {
      type: Array,
      required: true,
    },
  },
};
</script>

<docs>
Display a list of links
```
<script>
  export default {
    data() {
      return {
        links: [{label: 'Molgenis', href: 'https://molgenis.org'}]
      }
    }
  }
</script>
<template>
  <LinksList :items="links">
    <p>Sample content</p>
  </LinksList>
</template>

```
</docs>
