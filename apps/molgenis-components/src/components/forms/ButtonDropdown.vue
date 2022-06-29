<template>
  <!-- <span>
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
      class="dropdown-menu bg-white mg-dropdown"
      :class="{ show: display }"
      v-click-outside="toggle"
    >
      <div class="form-group dropdown-item">
        <slot :close="toggle" />
      </div>
    </div>
  </span> -->
  <span class="dropdown">
    <button
      ref="showInputButton"
      class="btn btn-outline-primary"
      :class="{ 'dropdown-toggle': !icon }"
      @click="toggle"
    >
      <span v-if="label">{{ label }}</span>
      <span v-if="icon" :class="'fa fa-' + icon + ' fa-lg ml-2'"></span>
    </button>
    <span
      v-if="display"
      class="mg-dropdown bg-white"
      ref="dropdown"
      v-click-outside="toggle"
    >
      <slot :close="toggle" />
    </span>
  </span>
</template>

<style scoped>
.mg-dropdown {
  position: absolute;
  z-index: 100;
}
</style>

<script>
import vClickOutside from "v-click-outside";
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
          modifiers: {
            offset: [0, 20],
            distance: 
          },
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
   <div>My content</div>
</ButtonDropdown>
<ButtonDropdown class="ml-3" label="Icon btn" icon="columns">
     <div>My image button content</div>
</ButtonDropdown>
<ButtonDropdown class="ml-3" label="with menu">
     <div>
       <a class="dropdown-item" href="#">Action</a>
       <a class="dropdown-item" href="#">Another action</a>
      <a class="dropdown-item" href="#">Something else here</a>
    </div>
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
