<template>
  <span>
    <div class="dropdown m-0 p-0" :class="{ show: display }">
      <button
        class="btn btn-outline-primary border-0"
        :class="{
          'dropdown-toggle': !icon,
          'nav-link': isMenuItem,
        }"
        type="button"
        data-toggle="dropdown"
        aria-haspopup="true"
        aria-expanded="false"
        @click="toggle"
      >
        <span v-if="label">{{ label }}</span>
        <span v-if="icon" :class="'fa fa-' + icon + ' fa-lg ml-2'"></span>
      </button>
    </div>
    <div
      v-if="display"
      class="dropdown-menu bg-white"
      style="position: absolute; z-index: 100"
      :class="{ show: display }"
      v-click-outside="toggle"
    >
      <div class="form-group dropdown-item">
        <slot :close="toggle" />
      </div>
    </div>
  </span>
</template>

<script>
import vClickOutside from "v-click-outside";

export default {
  directives: {
    clickOutside: vClickOutside.directive,
  },
  props: {
    label: String,
    icon: String,
    isMenuItem: Boolean,
  },
  data() {
    return {
      display: false,
    };
  },
  methods: {
    toggle() {
      this.display = !this.display;
    },
  },
};
</script>

<docs>
<template>
  <ButtonDropdown label="My pulldown">
    <ButtonAlt>Some button</ButtonAlt>
  </ButtonDropdown>
</template>
</docs>
