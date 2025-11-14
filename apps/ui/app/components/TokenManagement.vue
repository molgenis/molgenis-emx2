<template>
  <Modal v-model:visible="visible" :title="`Manage ${user.email}'s tokens`">
    <div class="overflow-y-auto">
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHead class="w-0"></TableHead>
            <TableHead>Name</TableHead>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow v-for="token in user.tokens">
            <TableCell>
<!--
              <Button
                    iconOnly
                    icon="trash"
                    type="secondary"
                    size="small"
                    label="Remove token"
                    @click="removeToken(token)"
                  />
-->
            </TableCell>
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
import { updateUser } from "~/util/adminUtils";
import Modal from "../../../tailwind-components/app/components/Modal.vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import Table from "../../../tailwind-components/app/components/Table.vue";
import TableHead from "../../../tailwind-components/app/components/TableHead.vue";
import TableRow from "../../../tailwind-components/app/components/TableRow.vue";
import TableCell from "../../../tailwind-components/app/components/TableCell.vue";
import TableHeadRow from "../../../tailwind-components/app/components/TableHeadRow.vue";
import { computed, ref } from "vue";
import type { IUser } from "~/util/adminUtils";
import _ from "lodash";

const emit = defineEmits(["userUpdated"]);

const props = defineProps<{
  user: IUser;
}>();

const visible = defineModel("visible", { required: true });

async function removeToken(token: string) {
  let editedUser: IUser = _.cloneDeep(props.user);
  editedUser.tokens = _.reject(props.user.tokens , (tok) => tok === token);
  await updateUser(editedUser);
  emit("userUpdated");
}
</script>
