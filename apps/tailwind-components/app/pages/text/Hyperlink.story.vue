<script setup lang="ts">
import TextHyperlink from "../../components/text/Hyperlink.vue";
</script>
<template>
  <div>
    <span>Some text with </span
    ><TextHyperlink
      to="../"
      label="an internal nuxt hyperlink"
      type="nuxt"
    /><span> in it.</span>
  </div>
  <div>
    <span>Some text with </span
    ><TextHyperlink
      to="../"
      label="an internal static hyperlink"
      type="static"
    /><span> in it.</span>
  </div>
  <div>
    <span>Some text with </span
    ><TextHyperlink
      to="https://molgenis.org"
      label="an external hyperlink"
      type="external"
    /><span> in it.</span>
  </div>
  <div>
    <span>Some text with </span
    ><TextHyperlink
      to="https://molgenis.org"
      label="an external hyperlink with icon"
      icon="handshake"
      type="external"
    /><span> in it.</span>
  </div>
  <div>
    <span>Some text with a hyperlink without label (</span
    ><TextHyperlink to="https://molgenis.org" type="external" /><span
      >) in it.</span
    >
  </div>
  <div class="text-heading-4xl">
    <span>Large text with </span
    ><TextHyperlink
      to="https://molgenis.org"
      label="an external hyperlink"
      type="external"
    /><span> in it.</span>
  </div>
</template>
