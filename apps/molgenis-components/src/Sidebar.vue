<template>
  <div id="sidebar-wrapper" class="border-right overflow-auto vh-100">
    <div class="sidebar-heading">Components</div>
    <div class="list-group list-group-flush">
      <li
        class="list-group-item"
        v-for="(key, index) in Object.keys(docsTree)"
        :key="index"
      >
        <strong> {{ key }}</strong>
        <a
          v-for="(item, index2) in docsTree[key]"
          :key="index2"
          href="#"
          v-scroll-to="camel2Kebab(item.name)"
          class="list-group-item"
          >{{ item.name }}</a
        >
      </li>

      <li class="list-group-item">
        <strong>Client</strong>
        <router-link to="/client" class="list-group-item">Client</router-link>
      </li>
    </div>
  </div>
</template>

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
    camel2Kebab(name) {
      return (
        "#" +
        name.replace(/[A-Z]/g, (letter, index) => {
          return index == 0 ? letter.toLowerCase() : "-" + letter.toLowerCase();
        })
      );
    },
  },
};
</script>

