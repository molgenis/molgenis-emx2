<template>
    <span>
        <div class="dropdown m-0 p-0" :class="{ show: display }">
            <button
                aria-expanded="false"
                aria-haspopup="true"
                class="btn btn-outline-primary border-0"
                :class="{
                    'dropdown-toggle': !icon,
                }"
                data-toggle="dropdown"
                type="button"
                @click="toggle"
            >
                <span v-if="label">{{ label }}</span>
                <span v-if="icon" :class="'fa fa-' + icon + ' fa-lg ml-2'" />
            </button>
        </div>
        <div
            v-if="display"
            v-click-outside="toggle"
            class="dropdown-menu bg-white"
            :class="{ show: display }"
            style="position: absolute; z-index: 100"
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
