<template>
  <div>
    <ButtonAction @click="toggle">
      Selection
      <span class="badge badge-light">
        {{ Array.isArray(selection) ? value.length : 0 }}
      </span>
    </ButtonAction>
    <LayoutModal v-if="expand" @close="expand = false" title="Show selection">
      <template v-slot:body>
        <span v-if="!Array.isArray(selection) || selection.length == 0">
          No items selected
        </span>
        <span
          v-else
          class="btn-outline-primary btn-sm mr-2"
          v-for="(item, idx) in selection"
          :key="JSON.stringify(item)"
        >
          {{ flattenObject(item) }}
          <IconAction icon="times" @click="selection.splice(idx, 1)" />
        </span>
      </template>
      <template v-slot:footer>
        <ButtonAlt
          v-if="Array.isArray(selection) && selection.length > 0"
          @click="selection.splice(0, selection.length)"
          >clear selection
        </ButtonAlt>
      </template>
    </LayoutModal>
  </div>
</template>

<script>
import ButtonAction from "../forms/ButtonAction";
import IconAction from "../forms/IconAction";
import ButtonAlt from "../forms/ButtonAlt";
import LayoutModal from "../layout/LayoutModal";

export default {
  components: {
    IconAction,
    ButtonAlt,
    LayoutModal,
    ButtonAction,
  },
  props: {
    value: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      selection: [],
      expand: false,
    };
  },
  created() {
    this.selection = this.value;
  },
  watch: {
    value() {
      this.selection = this.value;
    },
    selection() {
      this.$emit("input", this.selection);
    },
  },
  methods: {
    toggle() {
      this.expand = !this.expand;
    },
    flattenObject(object) {
      let result = "";
      Object.keys(object).forEach((key) => {
        if (object[key] === null) {
          //nothing
        } else if (typeof object[key] === "object") {
          result += this.flattenObject(object[key]);
        } else {
          result += "." + object[key];
        }
      });
      return result.replace(/^\./, "");
    },
  },
};
</script>
