<template>
  <div class="dropdown" :class="{ show: display }">
    <button
      class="btn btn-outline-primary border-0"
      :class="{
        'dropdown-toggle': !icon,
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
    <div
      v-if="display"
      class="dropdown-menu"
      :class="{ show: display }"
      v-click-outside="toggle"
    >
      <div class="form-group dropdown-item">
        <slot :close="toggle" />
      </div>
    </div>
  </div>
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
```
<ButtonDropdown label="My pulldown">
  <ButtonAlt>Some button</ButtonAlt>
</ButtonDropdown>
```
</docs>
