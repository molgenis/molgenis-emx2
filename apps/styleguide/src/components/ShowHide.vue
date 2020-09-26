<template>
  <div class="dropdown" :class="{ show: display }">
    <button
      class=" btn btn-link dropdown-toggle"
      type="button"
      data-toggle="dropdown"
      aria-haspopup="true"
      aria-expanded="false"
      @click="toggle"
    >
      <slot />
    </button>
    <div
      v-if="display"
      class="dropdown-menu"
      :class="{ show: display }"
      v-click-outside="toggle"
    >
      <div class="form-group dropdown-item" :key="timestamp">
        <div class="form-check" v-for="col in value" :key="col.name">
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
        <ButtonAlt @click="showAll">show all</ButtonAlt>
        <ButtonAlt @click="hideAll">hide all</ButtonAlt>
      </div>
    </div>
  </div>
</template>

<script>
import ButtonAlt from "./ButtonAlt";
import vClickOutside from "v-click-outside";

export default {
  components: { ButtonAlt },
  directives: {
    clickOutside: vClickOutside.directive
  },
  props: {
    value: {},
    checkAttribute: String
  },
  data() {
    return {
      display: false,
      timestamp: 0
    };
  },
  methods: {
    toggle() {
      this.display = !this.display;
    },
    emitValue() {
      this.$emit("input", this.value);
    },
    hideAll() {
      this.value.forEach(c => (c[this.checkAttribute] = false));
      this.emitValue();
    },
    showAll() {
      this.value.forEach(c => (c[this.checkAttribute] = true));
      this.emitValue();
    }
  }
};
</script>
