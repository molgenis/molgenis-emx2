<template>
  <Modal v-model:visible="visible" :title="`Manage ${userName}'s tokens`">
    <Table>
      <template #head>
        <TableHeadRow>
          <TableHead class="w-0"></TableHead>
          <TableHead>Name</TableHead>
        </TableHeadRow>
      </template>
      <template #body>
        <TableRow v-for="token in userTokens">
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

function closeModal() {
  visible.value = false;
}

const userName = ref<string>(props.user.email);
const userTokens = ref<string[]>(props.user.tokens || ([] as string[]));

async function removeToken(token: string) {
  let editedUser: IUser = _.cloneDeep(props.user);
  editedUser.tokens = _.reject(userTokens.value, (tok) => tok === token);
  await updateUser(editedUser);
  emit("userUpdated");
}
</script>
