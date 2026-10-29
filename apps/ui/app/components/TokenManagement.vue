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
import TableCell from "../../../tailwind-components/app/components/TableCell.vue";
import TableHead from "../../../tailwind-components/app/components/TableHead.vue";
import TableHeadRow from "../../../tailwind-components/app/components/TableHeadRow.vue";
import TableRow from "../../../tailwind-components/app/components/TableRow.vue";
import type { User } from "../interfaces/interfaces.ts";

const emit = defineEmits(["userUpdated"]);

const props = defineProps<{
  user: User;
}>();

const visible = defineModel("visible", { required: true });
</script>
