<template>
  <Modal v-model:visible="visible" :title="`Manage ${user.email}'s tokens`">
    <div class="overflow-y-auto">
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead>Name</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow v-for="token in user.tokens">
            <TableCell>
              {{ token }}
            </TableCell>
          </TableRow>
        </template>
      </Table>
    </div>
  </Modal>
</template>

<script setup lang="ts">
import Modal from "../../../tailwind-components/app/components/Modal.vue";
import Table from "../../../tailwind-components/app/components/Table.vue";
import TableHead from "../../../tailwind-components/app/components/TableHead.vue";
import TableRow from "../../../tailwind-components/app/components/TableRow.vue";
import TableCell from "../../../tailwind-components/app/components/TableCell.vue";
import TableHeadRow from "../../../tailwind-components/app/components/TableHeadRow.vue";
import type { IUser } from "~/util/adminUtils";

const emit = defineEmits(["userUpdated"]);

const props = defineProps<{
  user: IUser;
}>();

const visible = defineModel("visible", { required: true });
</script>
