<script setup lang="ts">
import { useRoute } from "#app/composables/router";
import { computed, ref } from "vue";
import type { IResources, IVariables } from "../../../interfaces/catalogue";
import { calcAggregatedHarmonisationStatus } from "~/utils/harmonisation";
import { getKey } from "../../utils/variableUtils";
import { resourceIdPath } from "../../utils/urlHelpers";
import {
  resourceToCartItem,
  variableToCartItem,
} from "../../utils/cartItem";
import { useCartStore } from "../../stores/useCartStore";
import Button from "../../../../tailwind-components/app/components/Button.vue";
import SideModal from "../../../../tailwind-components/app/components/SideModal.vue";
import TableSticky from "../table/Sticky.vue";
import HarmonisationTableCellAvailableIcon from "./HarmonisationTableCellAvailableIcon.vue";
import VariableDisplay from "../VariableDisplay.vue";
import type { IVariableMappings } from "~~/interfaces/types";
import CartButton from "../cart/CartButton.vue";

const route = useRoute();
const cartStore = useCartStore();

const props = defineProps<{
  variables: (IVariables & IVariableMappings)[];
  resources: IResources[];
}>();

const statusMap = computed(() =>
  calcAggregatedHarmonisationStatus(props.variables, props.resources)
);

let activeRowIndex = ref(-1);

// list of optional computed values that are non null when the side panel is shown
let showSidePanel = computed(() => activeRowIndex.value !== -1);
let activeVariable = computed(() =>
  showSidePanel ? props.variables[activeRowIndex.value] : null
);

let activeVariableKey = computed(() =>
  activeVariable.value ? getKey(activeVariable.value) : null
);

let activeVariablePath = computed(() =>
  activeVariableKey.value ? resourceIdPath(activeVariableKey.value) : ""
);
</script>

<template>
  <div class="mb-7 relative">
    <HarmonisationLegendMatrix size="small" />
    <div class="overflow-x-auto xl:max-w-table border-t">
      <TableSticky
        :columns="
          resources.sort((a, b) =>
            a.id.toLowerCase().localeCompare(b.id.toLowerCase())
          )
        "
        :rows="variables"
        class="h-screen overflow-auto"
      >
        <template #column="columnProps">
          <div class="flex flex-col items-center min-h-title max-h-title">
            <CartButton
              v-if="cartStore.isEnabled"
              :item="resourceToCartItem(columnProps.value)"
            />
            <div
              class="flex-1 min-h-0 hover:bg-gray-100 text-link font-normal min-w-[2rem] rotate-180 [writing-mode:vertical-lr] truncate hover:text-clip hover:overflow-visible"
            >
              <span
                class="hover:bg-gray-100 hover:flex items-center justify-items-end align-middle min-w-[2rem] hover:z-50 py-2"
              >
                {{ columnProps.value.id }}
              </span>
            </div>
          </div>
        </template>

        <template #row="rowProps">
          <div
            class="flex items-center text-body-base text-link font-normal hover:underline px-2 cursor-pointer"
            @click="activeRowIndex = rowProps.value.rowIndex"
          >
            <CartButton
              v-if="cartStore.isEnabled"
              :item="variableToCartItem(rowProps.value.row)"
              @click.stop
            />
            <span
              class="min-w-0 truncate hover:text-clip hover:overflow-visible hover:bg-gray-100 hover:border-r hover:pr-3 z-50"
            >
              {{ rowProps.value.row.name }}
            </span>
          </div>
        </template>

        <template #cell="cell">
          <HarmonisationTableCellAvailableIcon
            :status="
              ['complete', 'partial'].includes(
                statusMap?.[cell.value.rowIndex]?.[cell.value.columnIndex] ?? ''
              )
                ? 'available'
                : 'unmapped'
            "
            @click="activeRowIndex = cell.value.rowIndex"
          ></HarmonisationTableCellAvailableIcon>
        </template>
      </TableSticky>
    </div>

    <SideModal
      :key="activeRowIndex"
      :show="showSidePanel"
      :fullScreen="false"
      :slideInRight="true"
      @close="activeRowIndex = -1"
      buttonAlignment="right"
    >
      <template v-if="activeVariableKey">
        <VariableDisplay :variableKey="activeVariableKey" />
      </template>

      <template #footer>
        <CartButton
          v-if="activeVariable && cartStore.isEnabled"
          :item="variableToCartItem(activeVariable)"
          variant="button"
          size="small"
          class="mr-2.5"
        />
        <NuxtLink
          :to="`/${route.params.catalogue}/variables/${activeVariablePath}`"
        >
          <Button type="primary" size="small" label="More details " />
        </NuxtLink>
      </template>
    </SideModal>
  </div>
</template>
