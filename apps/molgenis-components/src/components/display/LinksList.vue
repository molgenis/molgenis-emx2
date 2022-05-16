<template>
  <ul class="list-group" :class="{ 'list-group-horizontal': isHorizontal }">
    <li class="list-group-item" v-for="(item, index) in items" :key="index">
      <span v-if="item.label" class="font-weight-bold pr-1">
        {{ item.label }}
      </span>
      <a v-if="item.text" :href="item.href">{{ item.text }}</a>
      <a v-else :href="item.href">{{ item.href }}</a>
    </li>
  </ul>
</template>

<style scoped>
ul.list-group li.list-group-item {
  border: none;
}

ul.list-group li.list-group-item {
  padding-left: 0px;
}

ul.list-group.list-group-horizontal li.list-group-item {
  padding-top: 0px;
  padding-bottom: 0px;
}
</style>

<script>
export default {
  name: "LinksList",
  props: {
    /**
     * List of link items
     * Link item has:
     * label: String
     * href: String (required)
     * text: String
     */
    items: {
      type: Array,
      required: true,
    },
    isHorizontal: {
      type: Boolean,
      required: false,
      default: () => false,
    },
  },
};
</script>

<docs>
<template>
  <div>
    Display a list of links
    <links-list
        :items="[{label: 'Molgenis', href: 'https://molgenis.org'}, {label: 'Google', href: 'https://google.com'}]"></links-list>
    With texts
    <links-list :items="[{label: 'Molgenis', href: 'https://molgenis.org', text: 'link 1'},
          {label: 'Vue', href: 'https://vuejs.org/', text: 'link 2'}]"><p>Sample content</p>
    </links-list>
    Display Horizontal
    <links-list
        :items="[{label: 'Molgenis', href: 'https://molgenis.org'}, {label: 'Google', href: 'https://google.com'}]"
        isHorizontal>
      <p>Sample content</p>
    </links-list>
  </div>
</template>
</docs>
