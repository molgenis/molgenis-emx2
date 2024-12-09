<template>
  <div>
    <div class="d-flex flex-row justify-content-between">
      <div>
        <h3>{{ service.name }}</h3>
      </div>
      <div>
        <Button
          type="outline"
          :label="isInCart ? 'Remove' : 'Add'"
          size="sm"
          @click="toggleSelection"
        />
      </div>
    </div>
    <table class="table-layout w-100">
      <tbody>
        <string :attribute="{ label: 'Id:', value: service.id }" />

        <string :attribute="{ label: 'Acronym:', value: service.acronym }" />

        <string
          :attribute="{
            label: 'Description:',
            value: service.description,
          }"
        />

        <string
          :attribute="{
            label: 'Description URL:',
            value: service.descriptionUrl,
          }"
        />

        <string :attribute="{ label: 'Device:', value: service.device }" />

        <string
          :attribute="{
            label: 'Device System:',
            value: service.deviceSystem,
          }"
        />

        <string
          :attribute="{
            label: 'Access Description URL:',
            value: service.accessDescriptionUrl,
          }"
        />

        <string
          :attribute="{
            label: 'Unit of Access:',
            value: service.unitOfAccess,
          }"
        />

        <string
          :attribute="{
            label: 'Access Description:',
            value: service.accessDescription,
          }"
        />

        <string
          :attribute="{
            label: 'Unit Cost:',
            value: service.unitCost,
          }"
        />

        <tr>
          <th scope="row" class="pr-1 align-top text-nowrap">Service Type:</th>
          <td>
            <div v-for="serviceType in service.serviceTypes">
              {{ serviceType.name }} ({{ serviceType.serviceCategory.name }})
            </div>
          </td>
        </tr>

        <tr v-if="service.tRL">
          <th scope="row" class="pr-1 align-top text-nowrap">TRL:</th>
          <td>
            {{ service.tRL.label ?? service.tRL.name }}
          </td>
        </tr>

        <quality v-if="service.qualityStandards" :attribute="qualityProps" />
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { computed, defineProps } from "vue";
import { IBiobanks, IServices } from "../../interfaces/directory";
import quality from "../../components/generators/view-components/quality.vue";
import string from "../../components/generators/view-components/string.vue";
import { useCheckoutStore } from "../../stores/checkoutStore";
import Button from "../../components/Button.vue";

const props = defineProps<{
  service: IServices;
  qualityProps: {
    label: string;
    value: string;
  };
}>();

const qualityProps = computed(() => {
  return {
    label: "Quality labels",
    value: props.service?.qualityStandards?.map((quality) => {
      return {
        label: quality.qualityStandard.label,
        certification_report: quality.certificationReport,
        certification_image_link: quality.certificationImageLink,
        quality_standard: quality.qualityStandard,
      };
    }),
  };
});

const isInCart = computed(() =>
  useCheckoutStore().isInCart(props.service.id ?? "")
);

function toggleSelection() {
  const bookmark = false;

  if (isInCart.value) {
    useCheckoutStore().removeServicesFromSelection(
      { name: props.service.biobank.name },
      [props.service.id],
      bookmark
    );
  } else {
    useCheckoutStore().addServicesToSelection(
      {
        id: props.service.biobank.id,
        name: props.service.biobank.name,
      } as IBiobanks,
      [{ label: props.service.name, value: props.service.id }],
      bookmark
    );
  }
}
</script>
