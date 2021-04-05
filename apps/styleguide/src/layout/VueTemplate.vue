<template>
  <runtime-template-compiler
    :template="`<div>` + template + `</div>`"
    v-bind="{ ...row }"
  />
</template>

<script>
import Vue from "vue";
import RuntimeTemplateCompiler from "vue-runtime-template-compiler";

Vue.use(RuntimeTemplateCompiler);

export default {
  props: {
    a: Object,
    template: String,
  },
};
</script>

<docs>
Example
```
<template>
  <VueTemplate :row="{name:'John'}" :template="template"/>
</template>
<script>
  export default {
    data() {
      return {
        template: "<h1>Hello {{row.name}}</h1>"
      }
    }
  }
</script>
```
</docs>
