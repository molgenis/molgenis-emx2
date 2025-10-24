<template>
  <div
    :id="`accordion-${id}`"
    :class="visible ? 'accordion visible' : 'accordion'"
  >
    <!-- Dynamic heading element (h1–h6) -->
    <component
      :is="headingTag"
      class="accordion-heading"
      :class="`heading-style-${headingStyle}`"
    >
      <button
        type="button"
        :id="`accordion-toggle-${id}`"
        class="accordion-toggle"
        :aria-controls="`accordion-content-${id}`"
        :aria-expanded="visible"
        @click="onclick"
      >
        <span class="toggle-label">{{ title }}</span>
        <ChevronDownIcon
          :class="visible ? 'toggle-icon rotated' : 'toggle-icon'"
        />
      </button>
    </component>

    <!-- Collapsible section -->
    <section
      :id="`accordion-content-${id}`"
      class="accordion-content"
      :aria-labelledby="`accordion-toggle-${id}`"
      role="region"
      v-show="visible"
    >
      <slot></slot>
    </section>
  </div>
</template>

<script>
import { ChevronDownIcon } from "@heroicons/vue/24/outline";

export default {
  name: "Accordion",
  components: { ChevronDownIcon },
  props: {
    id: {
      type: String,
      required: true,
    },
    title: {
      type: String,
      required: true,
    },
    isOpenByDefault: {
      type: Boolean,
      default: false,
    },
    /**
     * The heading style (1–6) determines which HTML tag (h1–h6) is rendered
     * and which color is applied.
     */
    headingStyle: {
      type: Number,
      default: 3,
      validator: (value) =>
        [1, 2, 3, 4, 5, 6].includes(value),
    },
  },
  data() {
    return {
      visible: false,
    };
  },
  computed: {
    headingTag() {
      return `h${this.headingStyle}`;
    },
  },
  methods: {
    onclick() {
      this.visible = !this.visible;
      this.$emit("isOpen", this.visible);
    },
  },
  mounted() {
    this.visible = this.isOpenByDefault;
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
      margin: 0;
      padding: 0;
      cursor: pointer;
      font-size: inherit;
      text-align: left;
      color: inherit; // ensure text uses heading color
      display: flex;
      justify-content: space-between;
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
      padding: 1.2rem;
    }
  }
}
</style>