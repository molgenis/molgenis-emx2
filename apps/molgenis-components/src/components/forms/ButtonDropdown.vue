<template>
  <span>
    <button
      type="button"
      ref="showInputButton"
      class="btn btn-outline-primary"
      :class="{ 'dropdown-toggle': !icon, 'nav-link': isMenuItem }"
      @click="toggle"
    >
      <span v-if="label">{{ label }}</span>
      <span v-if="icon" :class="'fa fa-' + icon + ' fa-lg ml-2'"></span>
    </button>
    <span
      v-if="display"
      class="mg-dropdown-drop bg-white border rounded"
      ref="dropdown"
      v-click-outside="toggle"
    >
      <slot :close="toggle" />
    </span>
  </span>
</template>

<style  scoped>
span.mg-dropdown-drop {
  /* bootstrap dropdown z-index */
  z-index: 1000;
}
</style>

<script>
import vClickOutside from "click-outside-vue3";
import Popper from "popper.js";

export default {
  directives: {
    clickOutside: vClickOutside.directive,
  },
  props: {
    label: String,
    icon: {
      type: String,
      required: false,
      default: "",
    },
    isMenuItem: Boolean,
    placement: {
      type: String,
      required: false,
      default: () => "bottom-start",
    },
  },
  data() {
    return {
      display: false,
      popperInstance: null,
    };
  },
  methods: {
    async toggle() {
      this.display = !this.display;
      if (this.display) {
        await this.$nextTick();
        const dropDownBtn = this.$refs["showInputButton"];
        const dropDownContent = this.$refs["dropdown"];
        this.popperInstance = new Popper(dropDownBtn, dropDownContent, {
          placement: this.placement,
          modifiers: { offset: { offset: "0,2px" } },
        });
      } else {
        this.popperInstance.destroy();
      }
    },
  },
};
</script>

<docs>
<template>
<demo-item>

  <ButtonDropdown label="Drop down">
    <div class="p-1">My content</div>
  </ButtonDropdown>

  <ButtonDropdown class="ml-3" label="Icon btn" icon="columns">
      <div class="p-1">My content</div>
  </ButtonDropdown>

  <ButtonDropdown class="ml-3" label="with menu">
      <div>
        <a class="dropdown-item" href="#">Action</a>
        <a class="dropdown-item" href="#">Another action</a>
        <a class="dropdown-item" href="#">Something else here</a>
      </div>
  </ButtonDropdown>

  <ButtonDropdown class="ml-3" label="with form">
    <form class="px-4 py-3" style="min-width: 320px;">
      <div class="form-group">
        <label for="exampleDropdownFormEmail1">Email address</label>
        <input type="email" class="form-control" id="exampleDropdownFormEmail1" placeholder="email@example.com">
      </div>
      <div class="form-group">
        <label for="exampleDropdownFormPassword1">Password</label>
        <input type="password" class="form-control" id="exampleDropdownFormPassword1" placeholder="Password">
      </div>
      <div class="form-group">
        <div class="form-check">
          <input type="checkbox" class="form-check-input" id="dropdownCheck">
          <label class="form-check-label" for="dropdownCheck">
            Remember me
          </label>
        </div>
      </div>
      <button type="submit" class="btn btn-primary">Sign in</button>
    </form>
  </ButtonDropdown>

  <h5 class="mt-2">Some text to check the z-index and display </h5>
  <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit
  , sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
   Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
  Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
  Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
  </p>
</demo-item>
  
</template>
</docs>
