<template>
  <span>
    <button
      class="btn p-1 mt-n1 mb-n1 btn-outline-primary border-0"
      :class="'btn-outline-' + color"
      @click.prevent="onClick"
      @mouseenter="showTooltip = true"
      @mouseleave="showTooltip = false"
    >
      <span v-if="label" class="mr-2">
        {{ label }}
      </span>
      <i :class="'fas fa-fw fa-' + icon" />
    </button>
    <div class="tooltip bs-tooltip-right show" role="tooltip">
      <div class="arrow"></div>
      <div class="tooltip-inner">
        <div>{{ tooltip }}</div>
      </div>
    </div>
  </span>
</template>

<style>
.tooltip {
  position: relative;
  top: 30px;
  left: 20px;
}

.arrow {
  top: 5px;
  left: 5px;
}
</style>

<script>
import ButtonAction from "./ButtonAction.vue";

/** Button that is shown as a icon. Choose font-awesome icon name as 'icon' parameter to show particular icon. */
export default {
  extends: ButtonAction,
  props: {
    label: String,
    icon: String,
    tooltip: String,
  },
  data() {
    return {
      showTooltip: false,
    };
  },
};
</script>

<docs>

<template>
  <div>
    <label>times</label>
    <demo-item>
      <icon-action icon="times" @click="alert('clicked')"></icon-action>
    </demo-item>
    <label>chevron-down</label>
    <demo-item>
      <icon-action icon="chevron-down" @click="alert('clicked')"></icon-action>
    </demo-item>
    <label>chevron-up</label>
    <demo-item>
      <icon-action icon="chevron-up" @click="alert('clicked')"></icon-action>
    </demo-item>
    <label>snowflake</label>
    <demo-item>
      <icon-action icon="snowflake" @click="alert('clicked')"></icon-action>
    </demo-item>
    <label>withToolTip</label>
    <demo-item>
      <icon-action icon="snowflake" @click="alert('clicked')" tooltip="this is a snowflake"></icon-action>
      some text tooltip should not move
    </demo-item>
  </div>
</template>

<script>
  export default {
    methods: {
      alert(text) {
        alert(text);
      }
    }
  };
</script>
</docs>
