<script setup lang="ts">
import Hyperlink from "~/components/text/Hyperlink.vue";
</script>
<template>
  <div>
    <span>Some text with </span
    ><Hyperlink to="./" label="an internal hyperlink" type="static" /><span>
      in it.</span
    >
  </div>
  <div>
    <span>Some text with </span
    ><Hyperlink to="./" label="an external hyperlink" /><span> in it.</span>
  </div>
  <div>
    <span>Some text with </span
    ><Hyperlink
      to="./"
      label="an external hyperlink with icon"
      icon="handshake"
    /><span> in it.</span>
  </div>
</template>
