<template>
  <span>
    <span v-if="focus" class="dropdown show" v-click-outside="toggleFocus">
      <span class="dropdown-menu show">
        <button
          class="btn btn-link dropdown-item"
          v-if="!required"
          key="empty-option"
          @click.prevent="select(undefined)"
        >
          &nbsp;
        </button>
        <button
          class="btn btn-link dropdown-item"
          v-for="option in options"
          :key="option"
          @click.prevent="select(option)"
          :class="{ 'text-primary': value === option }"
        >
          {{ option }}
        </button>
      </span>
    </span>
    <span
      @click="toggleFocus"
      @mouseover="hover = true"
      @mouseleave="hover = false"
      class="inline-select"
    >
      {{ value }}
      <IconAction icon="pencil-alt" class="hoverIcon" />
    </span>
  </span>
</template>

<style scoped>
.inline-select {
  min-width: 1em;
}
</style>

<script>
import BaseInput from "./BaseInput.vue";
import IconAction from "./IconAction.vue";
import vClickOutside from "v-click-outside";

export default {
  directives: {
    clickOutside: vClickOutside.directive,
  },
  extends: BaseInput,
  components: {
    IconAction,
  },
  props: {
    options: Array,
  },
  data() {
    return {
      focus: false,
    };
  },
  methods: {
    select(option) {
      this.toggleFocus();
      this.$emit("input", option);
    },
    toggleFocus() {
      this.focus = !this.focus;
    },
  },
};
</script>

<docs>
<template>
  <div>
    <DemoItem>
      <div>
        This selector is in place
        <InputSelectInplace
          id="input-select-inplace"
          label="Animals"
          v-model="check"
          :options="['lion', 'ape', 'monkey']"
        />
        in this sentence.
      </div>
      <div>Selected: {{ check }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        This in place selector is required.
        <InputSelectInplace
          id="input-select-inplace"
          label="Animals"
          required
          v-model="requiredCheck"
          :options="['lion', 'ape', 'monkey']"
        />
        And can not be put back to nothing selected.
      </div>
      <div>Selected: {{ requiredCheck }}</div>
    </DemoItem>
  </div>
</template>
<script>
export default {
  data: function () {
    return {
      check: null,
      requiredCheck: "lion",
    };
  },
};
</script>
</docs>
