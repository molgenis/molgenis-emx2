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
      <b>
        data (
        <ButtonAlt class="pl-0" @click="showAll(true)">all</ButtonAlt>
        <ButtonAlt class="p-0" @click="hideAll(true)">none</ButtonAlt>
        )
      </b>
      <div v-for="(col, key) in columns" :key="key">
        <b class="ml-0" v-if="col.name == 'mg_draft'">
          metadata (
          <ButtonAlt class="pl-0" @click="showAll(false)">all</ButtonAlt>
          <ButtonAlt class="p-0" @click="hideAll(false)">none</ButtonAlt>
          )
        </b>
        <div class="form-check" v-if="col.columnType != 'HEADING'">
          <input
            class="form-check-input"
            type="checkbox"
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
    hideAll(data) {
      let update = JSON.parse(JSON.stringify(this.columns));
      for (var key in update) {
        if (
          (data && !update[key].name.startsWith("mg_")) ||
          (!data && update[key].name.startsWith("mg_"))
        ) {
          update[key][this.checkAttribute] = false;
        }
      }
      this.$emit("update:columns", update);
    },
    showAll(data) {
      let update = JSON.parse(JSON.stringify(this.columns));
      for (var key in update) {
        if (
          (data && !update[key].name.startsWith("mg_")) ||
          (!data && update[key].name.startsWith("mg_"))
        ) {
          update[key][this.checkAttribute] = true;
        }
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
