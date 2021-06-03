<template>
  <div class="d-flex">
    <div v-if="description && !focus" v-html="stripHtml(description)" />
    <div v-else-if="!focus">
      CONSTANT type is empty. Please set text or html in 'description'.
    </div>
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

export default {
  components: { IconAction },
  props: {
    inplace: Boolean,
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
simple
```
<InputConstant description="<h2>hello world</h2>"/>
```
editable
```
<template>
  <div>
    <InputConstant :description.sync="description" :inplace="true"/>
    {{ description }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        description: "<h2>hello world</h2>"
      }
    }
  }
</script>
```
</docs>
