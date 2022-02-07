<template>
  <div>
    <ButtonAlt @click="toggle">
      Selection
      <i class="fa fa-lg fa-check-square-o"></i>
      <span
        v-if="selection && selection.filter((s) => s != null).length > 0"
        class="badge badge-pill badge-danger"
        style="top: -7px; left: -7px"
      >
        {{
          Array.isArray(selection)
            ? selection.filter((s) => s != null).length
            : 0
        }}
      </span>
    </ButtonAlt>
    <LayoutModal v-if="expand" @close="expand = false" title="Show selection">
      <template v-slot:body>
        <span
          v-if="
            !Array.isArray(selection) ||
            selection.filter((s) => s != null).length == 0
          "
        >
          No items selected
        </span>
        <span
          v-else
          class="btn-outline-primary btn-sm mr-2"
          v-for="(item, idx) in selection.filter((s) => s != null)"
          :key="JSON.stringify(item)"
        >
          {{ flattenObject(item) }}
          <IconAction icon="times" @click="deselect(idx)" />
        </span>
      </template>
      <template v-slot:footer>
        <ButtonAlt
          v-if="Array.isArray(selection) && selection.length > 0"
          @click="clear"
        >
          clear selection
        </ButtonAlt>
      </template>
    </LayoutModal>
  </div>
</template>

<script>
import IconAction from "../forms/IconAction";
import ButtonAlt from "../forms/ButtonAlt";
import LayoutModal from "../layout/LayoutModal";

export default {
  components: {
    IconAction,
    ButtonAlt,
    LayoutModal,
  },
  props: {
    selection: {
      type: Array,
      default: () => [],
    },
  },
  data() {
    return {
      expand: false,
    };
  },
  methods: {
    deselect(idx) {
      let update = this.selection;
      update.splice(idx, 1);
      this.$emit("update:selection", update);
    },
    clear() {
      let update = this.selection;
      update.splice(0, update.length);
      this.$emit("update:selection", update);
    },
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

<docs>
Example
```
<SelectionBox/>
```
</docs>
