<template>
  <address :class="cssClassNames">
    <span><strong>University Medical Centre Groningen</strong></span>
    <span>NESTOR Registry</span>
    <span>Department of Medical Genetics</span>
    <span>Antonius Deusinglaan 1, 9713 AV Groningen</span>
    <span>The Netherlands</span>
    <a href="mailto:support@molgenis.org" v-if="showEmail">
      support@molgenis.org
    </a>
  </address>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps({
  centerText: {
    type: Boolean,
    default: false,
  },
  showEmail: {
    type: Boolean,
    default: false,
  },
});

const cssClassNames = computed(() => {
  const css = ["address"];
  if (props.centerText) {
    css.push("address-text-center");
  }
  return css.join(" ");
});
</script>

<style>
.address {
  font-style: normal;
  font-size: 14pt;
  margin-left: 2.25em;
  line-height: 1.3;

  strong {
    color: #0a0a0a;
  }

  span {
    display: block;
  }

  &.address-text-center {
    text-align: center;
  }
}
</style>
