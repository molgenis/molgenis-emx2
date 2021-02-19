<template>
  <ButtonDropdown :icon="icon">
    <div>
      <ButtonAlt @click="showAll">show all {{ label }}</ButtonAlt>
      <ButtonAlt @click="hideAll">hide all {{ label }}</ButtonAlt>
      <div>
        <div
          class="form-check"
          v-for="(col, key) in columns"
          :key="col.name + col[checkAttribute] + timestamp"
        >
          <input
            class="form-check-input"
            type="checkbox"
            v-model="columns[key][checkAttribute]"
            @change="emitValue"
            :id="col.name"
          />
          <label class="form-check-label" :for="col.name">
            {{ col.name }}
          </label>
        </div>
      </div>
      {{ columns }}
    </div>
  </ButtonDropdown>
</template>

<script>
import ButtonAlt from "../forms/ButtonAlt";
import ButtonDropdown from "../forms/ButtonDropdown";

export default {
  components: { ButtonAlt, ButtonDropdown },
  props: {
    value: Array,
    label: String,
    icon: String,
    checkAttribute: String,
  },
  data() {
    return {
      timestamp: 0,
      columns: [],
    };
  },
  methods: {
    emitValue() {
      this.$emit("input", this.columns);
    },
    hideAll() {
      this.columns.forEach((c) => (c[this.checkAttribute] = false));
      this.timestamp = Date.now();
      this.emitValue();
    },
    showAll() {
      this.columns.forEach((c) => (c[this.checkAttribute] = true));
      this.timestamp = Date.now();
      this.emitValue();
    },
  },
  watch: {
    columns: {
      deep: true,
      handler() {
        this.emitValue();
      },
    },
    value() {
      this.columns = this.value;
    },
  },
  created() {
    this.columns = this.value;
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
