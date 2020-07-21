<template>
  <LayoutModal
    :show="true"
    :title="'Drop Column \'' + column + '\''"
    @close="$emit('close')"
  >
    <template v-slot:body>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-else-if="error">{{ error }}</MessageError>
      <div v-else>
        Removing column <strong>{{ column }}</strong> in table
        <strong>{{ table }}</strong> <br />Are you sure?
      </div>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="$emit('close')">Close</ButtonAlt>
      <ButtonAction v-if="!success && !success" @click="dropColumn"
        >Drop Column
      </ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import { request } from "graphql-request";

import {
  ButtonAction,
  ButtonAlt,
  LayoutModal,
  MessageError,
  MessageSuccess
} from "@mswertz/emx2-styleguide";

export default {
  components: {
    LayoutModal,
    MessageSuccess,
    MessageError,
    ButtonAction,
    ButtonAlt
  },
  props: {
    schema: String,
    table: String,
    column: String
  },
  data: function() {
    return {
      success: null,
      error: null
    };
  },
  methods: {
    dropColumn() {
      this.loading = true;
      this.success = null;
      this.error = null;
      request(
        "graphql",
        `mutation drop($table:String,$column:String){drop(columns:[{table:$table,column:$column}]){message}}`,
        {
          table: this.table,
          column: this.column
        }
      )
        .then(data => {
          this.success = data.drop.message;
          this.$emit("close");
        })
        .catch(error => {
          if (error.response && error.response.status === 403) {
            this.error = "Forbidden. Do you need to login?";
            this.showLogin = true;
          } else this.error = error;
        })
        .finally((this.loading = false));
    }
  }
};
</script>

<docs>
    Example
    ```
    <template>
        <ColumnDropModal
                v-if="show"
                schema="pet store"
                table="Pet"
                columnName="name"
                @close="show = false"
        />
        <ButtonAction v-else @click="show = true">Show</ButtonAction>
    </template>
    <script>
        export default {
            data: function () {
                return {
                    show: false
                };
            }
        };
    </script>
    ```
</docs>
