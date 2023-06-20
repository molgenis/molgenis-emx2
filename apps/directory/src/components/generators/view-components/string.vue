<template>
  <tr v-if="attribute && attribute.value && attribute.value.length">
    <th scope="row" class="pr-1 align-top text-nowrap">
      {{ displayName(attribute) }}
    </th>
    <td>
      <span>
        {{ attribute.value }}
      </span>

      <tooltip-component
        class="ml-2 copy-item d-inline"
        v-if="attribute.linkValue"
        text="Copy to clipboard"
        @click.prevent="copyToClipboard(attribute.linkValue)"
      >
        <span id="copy-icon" class="fa fa-clipboard"> </span>
      </tooltip-component>
    </td>
  </tr>
</template>

<script>
import TooltipComponent from "../../popovers/TooltipComponent.vue";

export default {
  components: {
    TooltipComponent,
  },
  props: {
    attribute: {
      type: Object,
    },
  },
  methods: {
    // ...mapMutations(["SetNotification"]),
    displayName(item) {
      return item.label || item.name || item.id;
    },
    copyToClipboard(link) {
      navigator.clipboard.writeText(link);
      this.SetNotification(`Copied ${link}`);
    },
  },
};
</script>

<style scoped>
.copy-item {
  width: 1rem;
}

.fa-clipboard {
  position: relative;
  font-size: large;
}

.fa-clipboard:hover {
  cursor: pointer;
}

.fa-external-link {
  top: 1px;
  position: relative;
}

.fa-external-link:hover {
  cursor: pointer;
}
</style>
