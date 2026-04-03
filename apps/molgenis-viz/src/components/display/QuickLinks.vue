<template>
  <nav :class="containerClasses">
    <ol :class="navClasses">
      <li class="nav-item" v-for="row in data">
        <LinkCard :imageSrc="imageSrc ? row[imageSrc] : null">
          <router-link :to="{ name: row[name] }">{{ row[label] }}</router-link>
        </LinkCard>
      </li>
      <!-- Slot for displaying additional links -->
      <slot></slot>
    </ol>
  </nav>
</template>

<script setup>
import { computed } from "vue";
import LinkCard from "./LinkCard.vue";

const props = defineProps({
  // an array of objects containing hrefs, labels, and background image paths
  // that are used in the molgenis-viz component LinkCard. This is designed to work with interal pages only. All links must defined in the router.js file. If you would like to render any other links, please use the default slot.
  data: {
    type: Array,
    default: [],
    required: true,
  },

  // The name of the property that contains the router name
  name: {
    type: String,
    required: true,
  },

  // The name of the property in each object that contains the text to display for each link
  label: {
    type: String,
    required: true,
  },

  // (optional) the name of the property that contains the location of an image. If supplied,
  // this will render the image in the background.
  imageSrc: {
    type: String,
  },

  // if True, child elements will be wrapped by row
  shouldWrap: {
    type: Boolean,
    default: false,
  },

  // specify the space between child elements
  linkGap: {
    type: Number,
    // `1`
    default: 1,
    validator: (value) => {
      return value >= 0 && value <= 5;
    },
  },
});

const containerClasses = computed(() => {
  const css = ["quick-links"];
  if (props.shouldWrap) {
    css.push("quick-links-wrapped");
  }
  return css.join(" ");
});

const navClasses = computed(() => {
  return `quick-links-nav gap-${props.linkGap}`;
});
</script>

<style lang="scss">
.quick-links {
  .quick-links-nav {
    display: flex;
    flex-direction: row;
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      flex-grow: 1;
    }

    .link-card {
      width: 100%;
    }
  }

  &.quick-links-wrapped {
    .quick-links-nav {
      flex-wrap: wrap;
    }
  }
}
</style>
