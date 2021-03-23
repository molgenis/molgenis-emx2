<template>
  <div v-if="show">
    <ButtonAlt @click="show = false">
      {{ hideLabel }}
    </ButtonAlt>
    <slot />
  </div>
  <ButtonAlt v-else @click="show = true">
    {{ showLabel }} <br>
  </ButtonAlt>
</template>

<script>
import ButtonAlt from '../forms/ButtonAlt.vue'

export default {
  components: {ButtonAlt},
  props: {
    title: {type: String, default: 'debug'},
  },
  data() {
    return {show: false}
  },
  computed: {
    hideLabel() {
      return 'hide ' + this.title
    },
    showLabel() {
      return 'show ' + this.title
    },
  },
}
</script>
