<template>
  <div class="d-flex form-group bg-white rounded p-2">
    <span v-if="!focus">
      <h1>{{ label }}</h1>
      <p>{{ description }}</p>
    </span>
    <textarea
      v-else
      v-focus="inplace || editMeta"
      :value="description"
      :class="{
        'form-control': true,
      }"
      :aria-describedby="id + 'Help'"
      @input="$emit('update:description', $event.target.value)"
      @blur="toggleFocus"
    />
    <div>
      <IconAction
        v-if="(inplace || editMeta) && !focus"
        class="hoverIcon align-top"
        icon="pencil-alt"
        @click="toggleFocus"
      />
    </div>
  </div>
</template>

<script>
import IconAction from "./IconAction";

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
    return { focus: false };
  },
  methods: {
    toggleFocus() {
      this.focus = !this.focus;
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
};
</script>

<docs>
structured, using format as parameter
```
<InputHeading label="About" description="My about section" />
```
editable
```
<template>
  <div>
    <InputHeading :description.sync="description" :label.sync="label" :inplace="true"/>
    {{ description }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        label: "About",
        description: "This is my about section"
      }
    }
  }
</script>
```
</docs>
