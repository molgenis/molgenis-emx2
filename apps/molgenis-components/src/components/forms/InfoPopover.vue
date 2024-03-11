<template>
  <div :class="{ 'font-weight-bold': boldText }">
    <div
      class="w3tooltip"
      tabindex="0"
      @click.prevent=""
      @mouseenter="activateTooltip"
      @mouseleave="deactivateTooltip"
      @focus="activateTooltip"
      @blur="deactivateTooltip"
      ref="tooltipTrigger"
    >
      <span v-if="label && !iconBeforeLabel">{{ label }}</span>
      <span
        class="fa d-inline-block"
        :class="[
          label ? (iconBeforeLabel ? 'mr-1' : 'ml-1') : '',
          faIcon,
          textColor,
        ]"
        aria-hidden="true"
      ></span>
      <span v-if="label && iconBeforeLabel">{{ label }}</span>
    </div>
    <teleport to="body">
      <span
        class="tooltiptext"
        :class="[placementClass, { active: isActive }]"
        ref="tooltipContent"
      >
        <slot />
      </span>
    </teleport>
  </div>
</template>

<script>
export default {
  name: "InfoPopover",
  props: {
    boldText: {
      type: Boolean,
      default: false,
    },
    faIcon: {
      type: String,
      default: "fa-question-circle",
    },
    textColor: {
      type: String,
      default: "text-info",
    },
    iconBeforeLabel: {
      type: Boolean,
      default: false,
    },
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
      isActive: false,
    };
  },
  computed: {
    placementClass() {
      return `tooltip-${this.popoverPlacement}`;
    },
  },
  methods: {
    activateTooltip() {
      this.updateTooltipPlacement();
      this.isActive = true;
    },
    deactivateTooltip() {
      this.isActive = false;
    },
    updateTooltipPlacement() {
      if (!this.$refs.tooltipTrigger || !this.$refs.tooltipContent) {
        return;
      }

      const triggerRect = this.$refs.tooltipTrigger.getBoundingClientRect();
      const contentRect = this.$refs.tooltipContent.getBoundingClientRect();
      const placementStyle = this.calculatePlacementStyle(
        triggerRect,
        contentRect
      );

      Object.assign(this.$refs.tooltipContent.style, placementStyle);
    },
    calculatePlacementStyle(triggerRect, contentRect) {
      const placementStyle = {};
      const offset = 8;

      switch (this.popoverPlacement) {
        case "right":
          placementStyle.left = `${
            triggerRect.right + window.scrollX + offset
          }px`;
          placementStyle.top = `${
            triggerRect.top +
            window.scrollY -
            (contentRect.height - triggerRect.height) / 2
          }px`;
          break;
        case "left":
          placementStyle.right = `${
            window.innerWidth - triggerRect.left + window.scrollX - offset
          }px`;
          placementStyle.top = `${
            triggerRect.top +
            window.scrollY -
            (contentRect.height - triggerRect.height) / 2
          }px`;
          break;
        case "bottom":
          placementStyle.top = `${
            triggerRect.bottom + window.scrollY + offset
          }px`;
          placementStyle.left = `${
            triggerRect.left +
            window.scrollX -
            (contentRect.width - triggerRect.width) / 2
          }px`;
          break;
        default:
          placementStyle.bottom = `${
            window.innerHeight - triggerRect.top + window.scrollY + offset
          }px`;
          placementStyle.left = `${
            triggerRect.left +
            window.scrollX -
            (contentRect.width - triggerRect.width) / 2
          }px`;
          break;
      }

      return placementStyle;
    },
  },
};
</script>
<style scoped>
.w3tooltip {
  position: relative;
  display: inline-block;
  cursor: pointer;
}
.tooltiptext {
  visibility: hidden;
  position: absolute;
  width: max-content;
  background-color: var(--dark);
  color: #fff;
  line-height: normal;
  padding: 6px 10px;
  border-radius: 6px;
  z-index: 1000;
  opacity: 0;
  transition: opacity 0.6s, visibility 0.6s;
  box-shadow: rgba(149, 157, 165, 0.2) 0px 8px 24px;
}
.tooltiptext.active {
  opacity: 1;
  visibility: visible;
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
  pointer-events: none;
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
  pointer-events: none;
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
  pointer-events: none;
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
  pointer-events: none;
}
</style>

<docs>
<template>
<demo-item>
   <div><small><i>Used in the directory app.</i></small></div>
    <InfoPopover
    faIcon="fa-regular fa-circle-check"
    textColor="text-success"
    class="ml-1 certificate-icon"
    popover-placement="right"
    >
    I am an info popover.
    </InfoPopover>
  </demo-item>
</template>

</docs>
