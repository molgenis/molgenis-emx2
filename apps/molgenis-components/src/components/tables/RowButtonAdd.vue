<template>
  <span v-if="subclasses">
    <IconAction icon="plus" @click="isModalShown = true">add</IconAction>
    <LayoutModal v-if="isModalShown && !subclassSelected">
      <template v-slot:body>
        <h5>Select subtype</h5>
        <p>
          {{ subclasses[0].label }} has several variant subtypes that allow for
          diffferent properties. Please select below the subtype to create.
        </p>
        <table class="table-bordered">
          <tr v-for="subclass in subclasses">
            <td>
              <a href="#" @click.prevent="subclassSelected = subclass">{{
                subclass.label
              }}</a>
            </td>
            <td>{{ subclass.description }}</td>
          </tr>
        </table>
      </template>
    </LayoutModal>
    <EditModal
      v-else-if="isModalShown"
      :id="id + 'add-modal'"
      :tableId="subclassSelected.id"
      :isModalShown="isModalShown"
      :schemaId="schemaId"
      :defaultValue="defaultValue"
      :visibleColumns="visibleColumns"
      :applyDefaultValues="true"
      @close="handleClose"
      @update:newRow="(event) => $emit('update:newRow', event)"
    />
  </span>
</template>

<script>
import IconAction from "../forms/IconAction.vue";
import { defineAsyncComponent } from "vue";
import Client from "../../client/client";
import LayoutModal from "../layout/LayoutModal.vue";

export default {
  name: "RowButtonAdd",
  components: {
    LayoutModal,
    IconAction,
    EditModal: defineAsyncComponent(() => import("../forms/EditModal.vue")),
  },
  props: {
    id: {
      type: String,
      required: true,
    },
    tableId: {
      type: String,
      required: true,
    },
    schemaId: {
      type: String,
      required: false,
    },
    defaultValue: {
      type: Object,
      required: false,
    },
    visibleColumns: {
      type: Array,
      required: false,
      default: () => null,
    },
  },
  data() {
    return {
      isModalShown: false,
      subclasses: [],
      subclassSelected: null,
    };
  },
  methods: {
    handleClose() {
      this.isModalShown = false;
      this.$emit("close");
    },
  },
  async created() {
    //check if subclasses
    const client = Client.newClient(this.schemaId);
    this.subclasses = [
      await client.fetchTableMetaData(this.tableId),
      ...(await client.fetchSubclassTables(this.tableId)),
    ];
    if (this.subclasses.length == 1) {
      this.subclassSelected = this.subclasses[0];
    }
  },
};
</script>

<docs>
<template>
  <div>
    <label for="row-add-btn-sample">composition of RowButton and EditModal configured for row add/insert</label>
    <div>
      <RowButtonAdd
          id="row-add-btn-sample"
          tableId="Pet"
          schemaId="pet store"
      />
    </div>
  </div>
</template>
</docs>
