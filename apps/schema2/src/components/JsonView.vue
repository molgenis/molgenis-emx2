<template>
  <ButtonAction v-if="!show" @click="show = true"> Show JSON</ButtonAction>
  <LayoutModal v-else @close="show = false">
    <template v-slot:body>
      <pre
        class="overflow-auto mh-100"
      ><code v-html="prettyPrint(value)" class="text-black"></code></pre>
    </template>
  </LayoutModal>
</template>
<style>
.json-key {
  color: black;
}

.json-value {
  color: brown;
}

.json-string {
  color: green;
}
</style>
<script>
import { ButtonAction, LayoutModal } from "molgenis-components";

export default {
  components: {
    ButtonAction,
    LayoutModal,
  },
  props: {
    value: Object,
  },
  data() {
    return {
      show: false,
    };
  },
  methods: {
    //thank you http://jsfiddle.net/unLSJ/
    replacer(match, pIndent, pKey, pVal, pEnd) {
      var key = "<span class=json-key>";
      var val = "<span class=json-value>";
      var str = "<span class=json-string>";
      var r = pIndent || "";
      if (pKey) r = r + key + pKey.replace(/[": ]/g, "") + "</span>: ";
      if (pVal) r = r + (pVal[0] == '"' ? str : val) + pVal + "</span>";
      return r + (pEnd || "");
    },
    prettyPrint(obj) {
      var jsonLine = /^( *)("[\w]+": )?("[^"]*"|[\w.+-]*)?([,[{])?$/gm;
      return JSON.stringify(obj, null, 2)
        .replace(/&/g, "&amp;")
        .replace(/\\"/g, "&quot;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(jsonLine, this.replacer);
    },
  },
};
</script>
