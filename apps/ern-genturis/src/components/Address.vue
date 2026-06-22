<script lang="ts" setup>
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
</script>

<template>
  <address class="address" :class="{ 'address-text-center': centerText }">
    <span><strong>University Medical Centre Groningen</strong></span>
    <span>GENTURIS registry</span>
    <span>Department of Medical Genetics</span>
    <span>Antonius Deusinglaan 1</span>
    <span>9713 AV Groningen</span>
    <span>The Netherlands</span>
    <a href="mailto:genturis-registry@umcg.nl" v-if="showEmail">
      genturis-registry@umcg.nl
    </a>
  </address>
</template>

<style lang="css" scoped>
.address {
  font-style: normal;
  font-size: 14pt;
  margin-left: 2.25em;
  line-height: 1.3;
}

.address span strong {
  color: var(--gray-900);
}

.address span {
  display: block;
}

.address.address-text-center {
  text-align: center;
}
</style>
