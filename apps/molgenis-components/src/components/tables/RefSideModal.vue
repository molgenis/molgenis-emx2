<template>
  <SideModal :label="label" :isVisible="isVisible" @onClose="emit('onClose')">
    {{ JSON.stringify(queryResult) }}
    <template v-slot:footer>
      <ButtonAction @click="emit('onClose')">Close</ButtonAction>
    </template>
  </SideModal>
</template>

<script lang="ts" setup>
import { ref, watch } from "vue";

const { client, isVisible, label, row, tableId } = defineProps({
  tableId: {
    type: String,
    default: "",
  },
  label: {
    type: String,
    default: "",
  },
  row: {
    type: String,
    default: "",
  },
  isVisible: {
    type: Boolean,
    default: true,
  },
  client: {
    type: Object,
    required: true,
  },
});

let queryResult = ref({});
const emit = defineEmits(["onClose"]);
watch([tableId, label, row], () => {
  updateData();
});

async function updateData() {
  if (tableId != "") {
    queryResult.value = await client
      .fetchRowData(tableId, { name: row })
      .catch(() => {});
  }
}
</script>

<style scoped></style>

<docs>
<template>
  <RefSideModal
    label="Label"
    tableId="Pet"
    :row="{}"
    :isVisible="showModal"
    @onClose="showModal = false"
  />
  <br /><button @click="showModal = !showModal">Toggle modal</button>
</template>
<script>
export default {
  data: function () {
    return {
      showModal: false,
    };
  },
};
</script>
</docs>
