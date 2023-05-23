<template>
  <button class="btn p-0 m-0 btn-outline-primary border-0" @click.prevent="onClick">
    <Tooltip :value="tooltip" class="mt-n1 mb-n1 p-1">
      <span v-if="label" class="mr-2">
        {{ label }}
      </span>
      <i :class="'fas fa-fw fa-' + icon" />
    </Tooltip>
  </button>
</template>

<script>
import ButtonAction from "./ButtonAction.vue";
import Tooltip from "./Tooltip.vue";

/** Button that is shown as a icon. Choose font-awesome icon name as 'icon' parameter to show particular icon. */
export default {
  extends: ButtonAction,
  components: {
    Tooltip,
  },
  props: {
    label: String,
    icon: String,
    tooltip: String,
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
    </demo-item>
  </div>
</template>

<script>
  export default {
    methods: {
      alert(text) {
        alert(text);
      },
    },
  };
</script>
</docs>
