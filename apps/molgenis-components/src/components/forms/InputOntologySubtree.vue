<template>
  <ul style="list-style-type: none">
    <li
      v-for="term in terms.filter((t) => t.visible)"
      :key="term.name + term.selected + term.expanded"
    >
      <!--show if selected or search-->
      <span @click.stop="toggleExpand(term)">
        <i
          class="fa-fw pl-2 pt-1 ml-3"
          role="button"
          :class="getExpandState(term)"
        />
      </span>
      <span @click.stop="toggleSelect(term)">
        <i
          class="fa-fw text-primary pl-2 pt-1"
          :class="getSelectState(term)"
          role="button"
        />
      </span>
      <span
        @click.stop="toggleExpandOrSelect(term)"
        class="flex-grow-1 pl-2"
        role="button"
      >
        {{ term.label ? term.label : term.name }}
        <span v-if="term.code">
          (<span v-if="term.codesystem"> {{ term.codesystem }}: </span>
          {{ term.code }})
        </span>
        <small v-if="term.definition" class="text-muted">
          <i> - {{ term.definition }}</i>
        </small>
        <span v-if="term.children && countVisibleChildren(term) > 0">
          ({{ countVisibleChildren(term) }})
        </span>
      </span>
      <InputOntologySubtree
        v-if="term.expanded"
        :terms="term.children"
        :isMultiSelect="isMultiSelect"
        @select="$emit('select', $event)"
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
    terms: {
      type: Array,
      required: true,
      default: () => [],
    },
    isMultiSelect: { type: Boolean, default: false },
  },
  methods: {
    countVisibleChildren(term) {
      if (term.children) {
        return term.children.filter((t) => t.visible).length;
      } else {
        return 0;
      }
    },
    getExpandState(term) {
      if (this.countVisibleChildren(term) == 0) {
        return "fas fa-angle-right invisible";
      } else if (term.expanded) {
        return "fas fa-angle-down";
      } else {
        return "fas fa-angle-right";
      }
    },
    //expensive?
    getAllChildNames(term) {
      let childNames = [];
      if (term.children) {
        term.children.forEach((childTerm) => {
          childNames.push(childTerm.name);
          childNames = childNames.concat(this.getAllChildNames(childTerm));
        });
      }
      return childNames;
    },
    getSelectState(term) {
      if (term.selected === "complete") {
        return this.isMultiSelect
          ? "fas fa-check-square"
          : "fas fa-check-circle";
      } else if (term.selected === "partial") {
        return this.isMultiSelect ? "far fa-check-square" : "far fa-circle";
      } else {
        return this.isMultiSelect ? "far fa-square" : "far fa-circle";
      }
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
      if (term.selected === "complete") {
        this.$emit("deselect", term.name);
      } else {
        this.$emit("select", term.name);
      }
    },
    toggleExpand(term) {
      this.$emit("toggleExpand", term.name);
    },
  },
};
</script>
