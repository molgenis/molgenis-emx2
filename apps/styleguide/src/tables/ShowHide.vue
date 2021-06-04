<template>
  <ButtonDropdown :icon="icon" :label="label" v-slot="scope">
    <IconAction
      icon="times"
      class="float-right"
      style="margin-top: -10px; margin-right: -10px"
      @click="scope.close"
    />
    <div>
      <h6>
        {{ label }}
      </h6>
      <ButtonAlt @click="showAll">show all</ButtonAlt>
      <ButtonAlt @click="hideAll">hide all</ButtonAlt>

      <div>
        <div class="form-check" v-for="(col, key) in columns" :key="key">
          <input
            class="form-check-input"
            type="checkbox"
            v-if="col.columnType != 'CONSTANT'"
            :checked="
              col[checkAttribute] == undefined
                ? defaultValue
                : col[checkAttribute]
            "
            @input.prevent="change(key, !col[checkAttribute])"
            :id="col.name"
          />
          <label class="form-check-label" :for="col.name">
            {{ col.name }}
          </label>
        </div>
      </div>
    </div>
  </ButtonDropdown>
</template>

<script>
import ButtonAlt from "../forms/ButtonAlt";
import ButtonDropdown from "../forms/ButtonDropdown";
import IconAction from "../forms/IconAction";

export default {
  components: { ButtonAlt, ButtonDropdown, IconAction },
  props: {
    columns: Array,
    label: String,
    icon: String,
    checkAttribute: String,
    defaultValue: { type: Boolean, default: false },
  },
  methods: {
    value(col) {
      return col[this.checkAttribute] == undefined
        ? this.defaultValue
        : col[this.checkAttribute];
    },
    change(key, value) {
      let update = JSON.parse(JSON.stringify(this.columns));
      update[key][this.checkAttribute] = value;
      this.$emit("update:columns", update);
    },
    hideAll() {
      let update = JSON.parse(JSON.stringify(this.columns));
      for (var key in update) {
        update[key][this.checkAttribute] = false;
      }
      this.$emit("update:columns", update);
    },
    showAll() {
      let update = JSON.parse(JSON.stringify(this.columns));
      for (var key in update) {
        update[key][this.checkAttribute] = true;
      }
      this.$emit("update:columns", update);
    },
  },
};
</script>

<docs>
Example
```
<ShowHide label="x">

</ShowHide>
```
</docs>
