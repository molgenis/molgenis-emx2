<template>
  <span
    v-if="terms && terms[0] != null"
    class="pb-2 mb-2"
    :style="
      inline ? 'font-size: small; vertical-align: super;' : 'display:block;'
    "
  >
    <span
      v-for="t in terms"
      :class="'font-weight-bold mr-2 mb-2 badge badge-' + color"
      style="font-size: 100%"
      @mouseover="hover = t.name"
      @mouseleave="hover = null"
    >
      {{ t.name }}
      <div
        v-if="hover == t.name && t.definition"
        class="tooltip bs-tooltip-bottom show"
        role="tooltip"
      >
        <div class="arrow"></div>
        <div class="tooltip-inner">
          {{ t.definition }}
          <span v-if="t.ontologyTermURI">
            (<a :href="t.ontologyTermURI">{{ t.code ? t.code : "code" }} </a>)
          </span>
        </div>
      </div>
    </span>
  </span>
  <p v-else-if="inline == false">N/A</p>
</template>

<script>
export default {
  props: {
    terms: Array,
    inline: { type: Boolean, default: () => false },
    color: {
      type: String,
      default: () => "primary",
    },
  },
  data() {
    return {
      hover: null,
    };
  },
};
</script>
