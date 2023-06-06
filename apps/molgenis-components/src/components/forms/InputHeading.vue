<template>
  <div>
    <span
      v-if="!focusLabel"
      class="d-flex form-group bg-white rounded pt-4 pb-1 mb-1"
    >
      <h1>{{ label }}</h1>
      <IconAction
        v-if="inplace || editMeta"
        class="hoverIcon align-top"
        icon="pencil-alt"
        @click="toggleFocusLabel"
      />
    </span>
    <input
      v-else
      :value="label"
      class="form-control"
      @input="$emit('update:label', $event.target.value)"
      @blur="toggleFocusLabel"
    />
    <div
      v-if="!focusDescription"
      class="d-flex form-group bg-white rounded pb-1 mb-1"
    >
      <p>{{ description }}</p>
      <IconAction
        v-if="inplace || editMeta"
        class="hoverIcon align-top"
        icon="pencil-alt"
        @click="toggleFocusDescription"
      />
    </div>
    <textarea
      v-else
      :value="description"
      :class="{
        'form-control': true,
      }"
      :aria-describedby="id + 'Help'"
      @input="$emit('update:description', $event.target.value)"
      @blur="toggleFocusDescription"
    />
  </div>
</template>

<script>
import IconAction from "./IconAction.vue";

/**
 * This is an component that does not have an input field but instead shows an header.
 * Otherwise it uses same mechanism as other input hence name 'InputHeader'
 */
export default {
  components: { IconAction },
  props: {
    inplace: Boolean,
    label: String,
    columnType: String,
    description: String,
    editMeta: Boolean,
  },
  data() {
    return { focusLabel: false, focusDescription: false };
  },
  methods: {
    toggleFocusLabel() {
      this.focusLabel = !this.focusLabel;
    },
    toggleFocusDescription() {
      this.focusDescription = !this.focusDescription;
    },
    stripHtml(input) {
      if (input) {
        return input.replace(
          /(<\/?(?:h1|h2|h3|h4|p|label|a)[^>]*>)|<[^>]+>/gi,
          "$1"
        );
      } else {
        return input;
      }
    },
  },
  emits: ["update:description", "update:label"],
};
</script>

<docs>
<template>
  <div>
    structured, using format as parameter
    <InputHeading label="About" description="My about section"/>
    editable
    <InputHeading v-model:description="description" v-model:label="label" :inplace="true"/>
  </div>
</template>
<script>
  export default {
    data() {
      return {
        label: 'About',
        description: 'This is my about section',
      };
    },
  };
</script>
</docs>
