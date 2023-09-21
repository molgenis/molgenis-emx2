<template>
  <div
    :id="`accordion-${id}`"
    :class="visible ? 'accordion visible' : 'accordion'"
  >
    <h3 class="accordion-heading">
      <button
        type="button"
        :id="`accordion-toggle-${id}`"
        class="accordion-toggle"
        :aria-controls="`accordion-content-${id}`"
        :aria-expanded="visible"
        v-on:click="onclick"
      >
        <span class="toggle-label">{{ title }}</span>
        <ChevronDownIcon
          :class="visible ? 'toggle-icon rotated' : 'toggle-icon'"
        />
      </button>
    </h3>
    <section
      :id="`accordion-content-${id}`"
      class="accordion-content"
      :aria-labelledby="`accordion-toggle-${id}`"
      role="region"
      v-show="visible"
    >
      <!-- Content to be hidden or shown -->
      <slot></slot>
    </section>
  </div>
</template>

<script>
import { ChevronDownIcon } from "@heroicons/vue/24/outline";

// @displayName Accordion
// Create a collapsible element for hiding and showing content. For example, the
// accordion component is a good option for structuring an FAQ page. Accordion state
// (i.e., open or closed) can be accessed using the following event `@isOpen`.
export default {
  props: {
    // A unique identifier for the accordion
    id: {
      type: String,
      required: true,
    },
    // A label that describes the hidden content
    title: {
      type: String,
      required: true,
    },
    // If true, the accordion will be opened on render
    isOpenByDefault: {
      type: Boolean,
      required: false,
      default: false,
    },
  },
  components: { ChevronDownIcon },
  data() {
    return {
      visible: false,
    };
  },
  methods: {
    onclick() {
      this.visible = !this.visible;
      this.$emit("isOpen", this.visible);
    },
  },
  mounted() {
    this.visible = this.isOpenByDefault ? this.isOpenByDefault : this.visible;
  },
};
</script>

<style lang="scss">
$border-radius: 6px;

.accordion {
  font-family: inherit;
  box-sizing: border-box;
  margin: 24px 0;
  border: 1px solid $gray-200;
  border-radius: $border-radius;

  .accordion-heading {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    margin: 0;
    padding: 16px 12px;
    font-size: 14pt;
    background-color: $gray-050;
    border-radius: $border-radius;

    .accordion-toggle {
      border: none;
      position: relative;
      background: none;
      background-color: none;
      margin: 0;
      padding: 0;
      cursor: pointer;
      font-size: inherit;
      text-align: left;
      color: currentColor;
      display: flex;
      justify-content: flex-start;
      align-items: center;
      width: 100%;

      $icon-size: 24px;
      .toggle-label {
        display: inline-block;
        width: calc(100% - $icon-size);
        word-break: break-word;
      }

      .toggle-icon {
        width: $icon-size;
        height: $icon-size;
        transform: rotate(0);
        transform-origin: center;
        transition: transform 0.4s ease-in-out;

        &.rotated {
          transform: rotate(180deg);
        }
      }
    }
  }

  .accordion-content {
    margin: 0;
    box-sizing: content-box;
  }

  &.visible {
    .accordion-heading {
      border-radius: $border-radius $border-radius 0 0;
    }
    .accordion-content {
      padding: 0 12px;
    }
  }
}
</style>
