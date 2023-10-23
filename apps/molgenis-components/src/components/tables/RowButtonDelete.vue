<template>
  <span>
    <RowButton type="delete" @delete="isModalShown = true" />
    <ConfirmModal
      v-if="isModalShown"
      :title="'Delete from ' + tableId"
      actionLabel="Delete"
      actionType="danger"
      :tableId="tableId"
      :pkey="pkey"
      @close="handleClose"
      @confirmed="handleExecuteDelete"
    />
  </span>
</template>

<script>
import RowButton from "./RowButton.vue";
import ConfirmModal from "../forms/ConfirmModal.vue";
import Client from "../../client/client.ts";

export default {
  name: "RowButtonDelete",
  components: { RowButton, ConfirmModal },
  props: {
    id: {
      type: String,
      required: true,
    },
    tableId: {
      type: String,
      required: true,
    },
    pkey: {
      type: Object,
    },
    schemaId: {
      type: String,
      required: false,
    },
  },
  data() {
    return {
      isModalShown: false,
      client: null,
    };
  },
  methods: {
    async handleExecuteDelete() {
      if (!this.client) {
        this.client = Client.newClient(this.schemaId);
      }
      await this.client
        .deleteRow(this.pkey, this.tableId)
        .then(() => {
          this.$emit("success", {
            deletedKey: this.pkey,
            deleteFrom: this.tableId,
          });
        })
        .catch((error) => {
          const errorMessage =
            error?.response?.data?.errors &&
            Array.isArray(error?.response?.data?.errors)
              ? error.response.data.errors[0].message
              : "Failed to delete row data from table ";

          this.$emit("error", { errorMessage, error });
        });
      this.handleClose();
    },
    handleClose() {
      this.isModalShown = false;
      this.$emit("close");
    },
  },
};
</script>

<docs>
<template>
  <div>
    <label for="row-delete-btn-sample">Row delete button with delete handler included</label>
    <div>
      <RowButtonDelete
          id="row-delete-btn-sample"
          tableId="Pet"
          :pkey="{name: 'pooky'}"
          schemaId="pet store"
          @error="handleError"
          @success="handleSuccess"
      />
    </div>
    <div v-if="error">
      <p class="text-danger">
        {{ error.errorMessage }}
      </p>
    </div>
    <div v-if="success">
      <p class="text-success">Success: delete {{ success.deletedKey }} from {{ success.deletedKey }}</p>
    </div>
    <button v-if="error || success" class="btn btn-secondary" @click="">clear</button>
  </div>
</template>
<script>
  export default {
    data() {
      return {
        error: null,
        success: null,
      };
    },
    methods: {
      handleError(error) {
        this.error = error;
      },
      handleSuccess(success) {
        this.success = success;
      },
      handleClear() {

      },
      handleClose() {
        this.isModalShown = false;
        this.$emit('close');
      },
    },
  };

</script>
</docs>
