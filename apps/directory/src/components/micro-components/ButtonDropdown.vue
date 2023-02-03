<template>
  <details
    class="drawer-button border border-secondary p-0"
    @click="toggleDrawer"
  >
    <summary>
      {{ buttonText }} <span class="fa-solid fa-caret-down"></span>
    </summary>
    <div>
      <div class="drawer bg-white">
        <slot />
      </div>
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
  },
  methods: {
    toggleDrawer(event) {
      if (event.target.hasAttribute(open)) {
        console.log("hello");
      }
    },
  },
};
</script>

<style scoped>
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

.drawer {
  position: absolute;
  top: 2.6rem;
  width: inherit;
  z-index: 1000;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.175);
}

:deep(.scrollable-content) {
  max-height: 15rem;
  overflow-y: auto;
  overflow-x: hidden;
  white-space: nowrap;
  padding: 0 0.5rem 0.5rem 0.5rem;
}

.drawer-button {
  border-radius: 4px;
}
</style>