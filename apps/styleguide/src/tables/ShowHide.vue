<template>
  <ButtonDropdown :icon="icon">
    <div>
      <ButtonAlt @click="showAll">show all {{ label }}</ButtonAlt>
      <ButtonAlt @click="hideAll">hide all {{ label }}</ButtonAlt>
      <div>
        <div
          class="form-check"
          v-for="col in value"
          :key="col.name + col[checkAttribute] + timestamp"
        >
          <input
            class="form-check-input"
            type="checkbox"
            @change="emitValue"
            v-model="col[checkAttribute]"
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

export default {
  components: { ButtonAlt, ButtonDropdown },
  props: {
    value: {},
    label: String,
    icon: String,
    checkAttribute: String
  },
  data() {
    return {
      timestamp: 0
    };
  },
  methods: {
    updateTimestamp() {
      this.timestamp = new Date().getTime();
    },
    emitValue() {
      this.$emit("input", this.value);
    },
    hideAll() {
      this.value.forEach(c => (c[this.checkAttribute] = false));
      this.updateTimestamp();
      this.emitValue();
    },
    showAll() {
      this.value.forEach(c => (c[this.checkAttribute] = true));
      this.updateTimestamp();
      this.emitValue();
    }
  }
};
</script>
