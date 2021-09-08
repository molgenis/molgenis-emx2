<template>
  <ul style="list-style-type: none">
    <li v-for="term in terms" :key="term.name">
      <div class="d-flex">
        <i
          class="fa-fw pl-2 pt-1"
          role="button"
          :class="expandState(term)"
          @click="toggleExpand(term)"
        />
        <i
          class="fa-fw text-primary pl-2 pt-1"
          :class="selectState(term)"
          @click="toggleSelect(term)"
          role="button"
        />
        <span
          @click="toggleExpandOrSelect(term)"
          class="flex-grow-1 pl-2"
          role="button"
        >
          {{ term.name }}
          <span v-if="term.children">({{ term.children.length }})</span></span
        >
      </div>
      <InputOntologySubtree
        v-if="expanded.indexOf(term.name) >= 0"
        :expanded="expanded"
        :selection="selection"
        :terms="term.children"
        :list="list"
        @select="select(term, $event)"
        @deselect="$emit('deselect', $event)"
        @toggleExpand="$emit('toggleExpand', $event)"
      />
    </li>
  </ul>
</template>

<script>
export default {
  name: "InputOntologySubtree",
  props: {
    terms: Array,
    selection: Array,
    expanded: Array,
    list: { type: Boolean, default: false },
  },
  computed: {
    hasChildren() {
      return this.terms.filter((t) => t.children).length > 0;
    },
  },
  methods: {
    select(self, items) {
      //if all children selected then also select 'self'
      if (self.children) {
        let allSelected = [].concat(this.selection, items);
        let allChildren = self.children.map((c) => c.name);
        if (allChildren.every((elem) => allSelected.indexOf(elem) > -1)) {
          items.push(self.name);
        }
      }
      this.$emit("select", items);
    },
    expandState(item) {
      if (!item.children) {
        return "fas fa-angle-right invisible";
      } else if (this.expanded.includes(item.name)) {
        return "fas fa-angle-down";
      } else {
        return "fas fa-angle-right";
      }
    },
    selectState(item) {
      if (this.selection.includes(item.name)) {
        return this.list ? "fas fa-check-square" : "fas fa-check-circle";
      } else if (
        item.children &&
        item.children
          .map((c) => c.name)
          .some((c) => this.selection.indexOf(c) != -1)
      ) {
        return this.list ? "far fa-check-square" : "far fa-circle";
      } else {
        return this.list ? "far fa-square" : "far fa-circle";
      }
    },
    getAllChildNames(term) {
      let result = [term.name];
      if (term.children) {
        term.children.forEach((t) => {
          result.push(...this.getAllChildNames(t));
        });
      }
      return result;
    },
    toggleExpandOrSelect(term) {
      //if node  expand,
      if (term.children) {
        this.toggleExpand(term);
      }
      //leafs select
      else {
        this.toggleSelect(term);
      }
    },
    toggleSelect(term) {
      //if selecting then also expand
      //if deselection we keep it open
      if (this.selection.indexOf(term.name) === -1) {
        if (this.expanded.indexOf(term.name) === -1) {
          this.$emit("toggleExpand", term.name);
        }
        //select children, recursively in case of list
        if (this.list) {
          this.$emit("select", this.getAllChildNames(term));
        } else {
          this.$emit("select", [term.name]);
        }
      } else {
        this.$emit("deselect", this.getAllChildNames(term));
      }
    },
    toggleExpand(term) {
      this.$emit("toggleExpand", term.name);
    },
  },
};
</script>
