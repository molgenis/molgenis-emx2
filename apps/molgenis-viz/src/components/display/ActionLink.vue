<template>
  <a
    class="action-link"
    :href="href"
    :target="showExternalLinkIcon ? '_blank' : false"
  >
    <slot></slot>
    <ArrowTopRightOnSquareIcon v-if="showExternalLinkIcon" class="link-icon" />
  </a>
</template>

<script>
import { ArrowTopRightOnSquareIcon } from "@heroicons/vue/24/outline";

// Render a hyperlink styled like a button. Generic text can be displayed using
// the default slot. If applicable, you may render an 'external link' icon using
// argument `showExternalLinkIcon`.
export default {
  props: {
    // String containing a url to a page
    href: {
      type: String,
      required: true,
    },
    // If `true`, an svg icon will be displayed after the text
    showExternalLinkIcon: {
      type: Boolean,
      default: false,
    },
  },
  components: { ArrowTopRightOnSquareIcon },
};
</script>

<style lang="scss">
.action-link {
  display: block;
  width: 100%;
  text-align: center;
  color: $molgenis-blue-800;
  box-sizing: border-box;
  padding: 0.5em 1em;
  border: 1px solid $molgenis-blue-800;
  background-color: $molgenis-blue-050;
  border-radius: 6px;
  margin: auto;

  .link-icon {
    margin-left: 3px;
    margin-top: -2px;
  }

  &:hover,
  &:focus {
    outline: 1px solid;
  }
}
</style>
