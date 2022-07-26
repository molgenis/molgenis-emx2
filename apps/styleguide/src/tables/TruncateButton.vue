<template>
  <LayoutModal v-if="open" :title="title" :show="true" @close="$emit('close')">
    <template v-slot:body>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-else-if="error">{{ error }}</MessageError>
      <div v-else>
        Truncate
        <strong>{{ table }}</strong>
        <br/>Are you sure that you want to remove ALL rows in table '{{ table }}'?
        <br/>
      </div>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="closeForm">Close</ButtonAlt>
      <ButtonDanger v-if="!success && !success" @click="executeTruncate">
        Truncate
      </ButtonDanger>
    </template>
  </LayoutModal>
  <IconDanger v-else icon="bomb" @click="openForm">truncate</IconDanger>
</template>

<script>
import LayoutModal from "../layout/LayoutModal.vue";
import IconDanger from "../forms/IconDanger";
import ButtonDanger from "../forms/ButtonDanger";
import ButtonAlt from "../forms/ButtonAlt";
import MessageSuccess from "../forms/MessageSuccess";
import MessageError from "../forms/MessageError";
import {request} from "graphql-request";

export default {
  components: {
    LayoutModal, IconDanger, ButtonDanger, ButtonAlt, MessageSuccess, MessageError
  },
  props: {
    table: String,
    graphqlURL: {type: String, default: "graphql"},
  }, data() {
    return {
      open: false,
      success: null,
      error: null,
    };
  },
  computed: {
    title() {
      return `Truncate ${this.table}`;
    },
  },
  methods: {
    openForm() {
      this.open = true;
      this.error = null;
      this.success = null;
    },
    closeForm() {
      this.open = false;
      this.$emit("close");
    },
    executeTruncate() {
      let query = `mutation {truncate(tables:"${this.table}"){message}}`;
      request(this.graphqlURL, query)
          .then((data) => {
            this.success = data.truncate.message;
          })
          .catch((error) => {
            this.error = error.response.errors[0].message;
          });
    },
  }
}
</script>

<docs>
```
<TruncateButton table="Orders"/>
```
</docs>