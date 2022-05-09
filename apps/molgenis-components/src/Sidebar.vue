<template>
  <div id="sidebar-wrapper" class="border-right overflow-auto vh-100">
    <div class="sidebar-heading">Components</div>
    <div class="list-group list-group-flush">
      <li
        class="list-group-item"
        v-for="(key, index) in Object.keys(docsTree)"
        :key="index"
      >
        <strong @click="toggleMenuItem(key)">
          <a href="#">{{ key }} </a></strong
        >
        <div v-if="expandedMenuKeys.includes(key)">
          <a
            v-for="(item, index2) in docsTree[key]"
            :key="index2"
            href="#"
            v-scroll-to="anchor(item.name)"
            class="list-group-item"
            >{{ item.name }}</a
          >
        </div>
      </li>

      <li class="list-group-item">
        <strong>
          <router-link to="/client">Client</router-link>
        </strong>
      </li>
    </div>
  </div>
</template>

<style scoped>
.list-group-item {
  border: 0;
}
</style>

<script>
export default {
  name: "Sidebar",
  props: {
    /**
     * Key value object that has a String key for each component-doc
     * and value with component docs details ( name and path)
     */
    docsMap: Object,
  },
  data() {
    return {
      expandedMenuKeys: [],
    };
  },
  computed: {
    docsTree() {
      const docTree = {};
      const docItems = Object.values(this.docsMap);

      const addToTree = (subTree, docItem) => {
        if (!docItem.path.length) {
          subTree[docItem.name] = docItem;
        } else {
          const currentPath = docItem.path[0];
          docItem.path = docItem.path.slice(1);
          if (!subTree[currentPath]) {
            subTree[currentPath] = {};
          }

          addToTree(subTree[currentPath], docItem);
        }
      };

      docItems.forEach((docItem) => {
        addToTree(docTree, docItem);
      });

      return docTree;
    },
  },
  methods: {
    anchor(name) {
      return "#" + name;
    },
    toggleMenuItem(key) {
      if (this.expandedMenuKeys.includes(key)) {
        this.expandedMenuKeys.splice(this.expandedMenuKeys.indexOf(key), 1);
      } else {
        this.expandedMenuKeys.push(key);
      }
    },
  },
};
</script>
