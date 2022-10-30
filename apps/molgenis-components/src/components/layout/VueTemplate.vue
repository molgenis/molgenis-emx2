<script>
import {h} from 'vue';

export default {
  props: {
    row: Object,
    template: String,
  },
  // render(createElement) {
  //   return h(compile(`<div>${this.template}</div>`).call(this,createElement));
  // }
  render() {
    //const provide = this.$parent.$.provides ? this.$parent.$.provides : {}; // Avoids Vue warning

    const render = {
      template: `<div>${this.template}</div>`,
      data: () => ({
        row: this.row
      })
    }
    return h(render);
  }
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
