<template>
  <span>
    <div v-if="focus" class="dropdown show" v-click-outside="toggleFocus">
      <div class="dropdown-menu show">
        <button
          class="btn btn-link dropdown-item"
          v-if="!required"
          :key="option"
          @click.prevent="select(option)"
          :class="{ 'text-primary': value == undefined }"
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
      </div>
    </div>
    <span
      @click="toggleFocus"
      @mouseover="hover = true"
      @mouseleave="hover = false"
      style="min-width: 1em"
      class="d-flex flex-nowrap"
    >
      {{ value }}
      <IconAction icon="pencil-alt" class="hoverIcon" />
    </span>
  </span>
</template>

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
  methods: {
    select(option) {
      this.toggleFocus();
      this.$emit("input", option);
    },
    toggleFocus() {
      this.focus = !this.focus;
    },
  },
  data() {
    return {
      focus: false,
    };
  },
  props: {
    options: Array,
  },
};
</script>

<docs>
<template>
  <div>
    <DemoItem>
      <div> InputSelectInplace </div>
      <InputSelectInplace
        id="input-select-inplace"
        label="Animals"
        v-model="check"
        :options="['lion', 'ape', 'monkey']"
      />
    </DemoItem>
    <DemoItem>
      <div> InputSelectInplace - required </div>
      <InputSelectInplace
        id="input-select-inplace"
        label="Animals"
        required
        v-model="check"
        :options="['lion', 'ape', 'monkey']"
      />
    </DemoItem>
      Selected: {{ check }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        check: null
      };
    }
  };
</script>
</docs>
