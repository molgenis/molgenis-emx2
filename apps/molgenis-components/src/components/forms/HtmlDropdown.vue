<template>
  <details class="dropdown-button p-0" :class="buttonstateClass">
    <summary>
      {{ buttonText }}
      <slot name="counter"></slot>
      <span class="fa-solid fa-caret-down"></span>
    </summary>
    <div>
      <div class="dropdown bg-white" :class="containerstateClass" @click.stop>
        <slot />
      </div>
      <div class="close" @click="closeSelf"></div>
    </div>
  </details>
</template>

<script>
export default {
  props: {
    buttonText: {
      type: String,
      required: true,
    },
    active: {
      type: Boolean,
      required: false,
      default: () => false,
    },
  },
  methods: {
    closeSelf(event) {
      event.target.parentElement.parentElement.removeAttribute("open");
    },
  },
  computed: {
    buttonstateClass() {
      return this.active
        ? "bg-secondary text-white"
        : "border border-secondary";
    },
    containerstateClass() {
      return this.active ? "text-dark" : "";
    },
  },
};
</script>

<style scoped>
.close {
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
  left: 0;
  opacity: 0;
}

.close:hover {
  cursor: pointer;
}

details {
  position: relative;
  display: inline-block;
  white-space: nowrap;
}
details:hover {
  cursor: default;
}

details:hover > summary,
details:active > summary {
  background-color: var(--secondary);
  color: white;
}

details > summary:hover {
  cursor: pointer;
}

details:not([open]):hover {
  cursor: pointer;
}

details[open] > summary {
  background-color: var(--secondary);
  color: white;
}

details > summary {
  display: table;
  padding: 0.5rem;
}
details > summary::-webkit-details-marker {
  display: none;
}

details.align-right > *:not(summary) {
  right: 0;
}

.dropdown {
  position: absolute;
  top: 2.6rem;
  width: inherit;
  z-index: 9000;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.175);
}

:deep(.scrollable-content) {
  max-height: 15rem;
  overflow-y: auto;
  overflow-x: hidden;
  white-space: nowrap;
  padding: 0 0.5rem 0.5rem 0.5rem;
}

.dropdown-button {
  border-radius: 4px;
}
</style>

<docs>
<template>
<demo-item>
<div><small><i>Used in the directory app.</i></small></div>
<HtmlDropdown
  :button-text="'Html Dropdown'"
  :active="false"
  >
  <template v-slot:counter>
    <span class="badge badge-light border mr-2 ml-1">
      10
    </span>
  </template>
  <div class="d-flex flex-column pr-3 pl-1">
    <label><input type="checkbox" class="mr-1">Option 1</label>
    <label><input type="checkbox" class="mr-1">Option 2</label>
    <label><input type="checkbox" class="mr-1">Option 3</label>
    <label><input type="checkbox" class="mr-1">Option 4</label>
    <label><input type="checkbox" class="mr-1">Option 5</label>
  </div>
</HtmlDropdown>

</demo-item>
  
</template>
</docs>
