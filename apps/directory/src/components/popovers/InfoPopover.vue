<template>
  <div
    :class="{
      'font-weight-bold': boldText,
    }"
  >
    <div class="w3tooltip" tabindex="0" @click.prevent="">

      <span v-if="label && !iconBeforeLabel">{{ label }}</span>
      <span
        :ref="uniqueLabelId"
        class="fa d-inline-block"
        :class="[
          label ? (iconBeforeLabel ? 'mr-1' : 'ml-1') : '',
          faIcon,
          textColor,
        ]"
        aria-hidden="true"
      ></span>
      <span :ref="uniqueLabelId" v-if="label && iconBeforeLabel">{{ label }}</span>

      <span
        class="tooltiptext"
        :ref="uniqueContentsId"
        :class="placementClass"
        :style="placementStyle"
        ><slot
      /></span>
    </div>
  </div>
</template>

<script>
export default {
  name: "InfoPopover",
  props: {
    /**
     * Wether the label is bold
     */
    boldText: {
      type: Boolean,
      required: false,
      default: false,
    },
    faIcon: {
      type: String,
      required: false,
      default: () => "fa-question-circle",
    },
    textColor: {
      type: String,
      required: false,
      default: () => "text-info",
    },
    /**
     * If set the ( ? ) icon wil be shown before the label
     */
    iconBeforeLabel: {
      type: Boolean,
      required: false,
      default: false,
    },
    /**
     * The text to show before or after the ( ? ) icon
     */
    label: {
      type: String,
      required: false,
    },
    popoverPlacement: {
      type: String,
      default: "top",
    },
  },
  data() {
    return {
      uniqueLabelId: `label-${Date.now() + "" + Math.random()}`,
      uniqueContentsId: `content-${Date.now() + "" + Math.random()}`,
      elementWidth: 0,
      elementHeight: 0,
      contentWidth: 0,
    };
  },
  computed: {
    placementClass() {
      return `tooltip-${this.popoverPlacement}`;
    },
    placementStyle() {
      switch (this.popoverPlacement) {
        case "right": {
          const top = 0;
          const left = this.elementWidth + 8;

          return `top: ${top}px;left:${left}px;`;
        }
        case "left": {
          const top = 0;
          const right = this.elementWidth + 8;

          return `top: ${top}rem;right:${right}px;`;
        }
        case "bottom": {
          const top = this.elementHeight + 8;
          const left = (this.elementWidth / 2) - (this.contentWidth / 2);
          return `top: ${top}px;left:${left}px;`;
        }
        default: {
          const bottom = this.elementHeight + 8;
          const right = (this.elementWidth / 2) - (this.contentWidth / 2);
          return `bottom: ${bottom}px;right:${right}px;`;
        }
      }
    },
  },
  mounted() {
    const tooltipEl = this.$refs[this.uniqueLabelId];
    this.elementHeight = tooltipEl.clientHeight;
    this.elementWidth = tooltipEl.clientWidth;
    this.contentWidth = this.$refs[this.uniqueContentsId].clientWidth;
  },
};
</script>
<style scoped>
.w3tooltip {
  position: relative;
  display: inline-block;
}
.w3tooltip .tooltiptext {
  visibility: hidden;
  position: absolute;
  width: auto;
  background-color: var(--dark);
  color: #fff;
  text-align: center;
  padding: 0.8rem 0.6rem;
  border-radius: 6px;
  z-index: 1;
  opacity: 0;
  white-space: nowrap;
  transition: opacity 0.6s;
}
.w3tooltip:hover {
  cursor: pointer;
}

.w3tooltip:hover .tooltiptext,
.w3tooltip:focus .tooltiptext {
  visibility: visible;
  opacity: 1;
}

.tooltip-right::after {
  content: "";
  position: absolute;
  top: 50%;
  right: 100%;
  margin-top: -5px;
  border-width: 5px;
  border-style: solid;
  border-color: transparent var(--dark) transparent transparent;
}

.tooltip-bottom::after {
  content: "";
  position: absolute;
  bottom: 100%;
  left: 50%;
  margin-left: -5px;
  border-width: 5px;
  border-style: solid;
  border-color: transparent transparent var(--dark) transparent;
}

.tooltip-top::after {
  content: "";
  position: absolute;
  top: 100%;
  left: 50%;
  margin-left: -5px;
  border-width: 5px;
  border-style: solid;
  border-color: var(--dark) transparent transparent transparent;
}

.tooltip-left::after {
  content: "";
  position: absolute;
  top: 50%;
  left: 100%;
  margin-top: -5px;
  border-width: 5px;
  border-style: solid;
  border-color: transparent transparent transparent var(--dark);
}
</style>
