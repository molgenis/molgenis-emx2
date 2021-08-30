<template>
  <div class="d-flex form-group bg-white rounded p-2">
    <span v-if="!focus && columnType == 'H1'">
      <h1>{{ label }}</h1>
      <p>{{ description }}</p>
    </span>
    <span v-else-if="!focus && columnType == 'H2'">
      <h2>{{ label }}</h2>
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
  },
};
</script>

<docs>
structured, using format as parameter
```
<InputConstant label="About" description="My about section" columnType="H2"/>
```
editable
```
<template>
  <div>
    <InputConstant :description.sync="description" :label.sync="label" columnType="H2" :inplace="true"/>
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
