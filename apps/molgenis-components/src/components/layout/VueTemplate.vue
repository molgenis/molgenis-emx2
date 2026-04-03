<script>
import { h } from "vue";

export default {
  props: {
    row: Object,
    template: String,
  },
  render() {
    const render = {
      template: `<div>${this.template}</div>`,
      data: () => ({
        row: this.row,
      }),
    };
    return h(render);
  },
};
</script>

<docs>

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

</docs>
