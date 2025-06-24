<template>
  <div class="container">
    <div v-if="service" class="container-fluid">
      <div class="row">
        <div class="col my-3 shadow-sm d-flex p-2 align-items-center bg-white">
          <Breadcrumb
            class="directory-nav"
            :crumbs="{
              ['Back to catalogue']: '../#/',
              [service.biobank.name]: `../biobank/${service.biobank.id}`,
              [service.name]: `../#/service/${service.id}`,
            }"
            useRouterLink
          />
          <check-out class="ml-auto" :bookmark="false" />
        </div>
      </div>

      <div class="row">
        <div class="col p-0">
          <div class="container p-0">
            <div class="row">
              <div class="col-md-8">
                <div class="d-flex flex-row justify-content-between">
                  <ReportTitle type="Service" :name="service.name" />
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

                    <string
                      :attribute="{ label: 'Name:', value: service.name }"
                    />

                    <string
                      :attribute="{ label: 'Acronym:', value: service.acronym }"
                    />

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

                    <string
                      :attribute="{ label: 'Device:', value: service.device }"
                    />

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
                      <th scope="row" class="pr-1 align-top text-nowrap">
                        Service Type:
                      </th>
                      <td>
                        <div v-for="serviceType in service.serviceTypes">
                          {{ serviceType.name }} ({{
                            serviceType.serviceCategory.name
                          }})
                        </div>
                      </td>
                    </tr>

                    <tr v-if="service.tRL">
                      <th scope="row" class="pr-1 align-top text-nowrap">
                        TRL:
                      </th>
                      <td>
                        {{ service.tRL.label ?? service.tRL.name }}
                      </td>
                    </tr>

                    <quality
                      v-if="service.qualityStandards"
                      :attribute="qualityProps"
                    />
                  </tbody>
                </table>
              </div>
              <!-- Right side card -->
              <div class="col-md-4">
                <div class="card">
                  <div class="card-body">
                    <div class="card-text">
                      <h5>Contact Information</h5>
                      <ul class="right-content-list">
                        <li v-if="service.contactInformation">
                          <ContactInformation
                            :contactInformation="service.contactInformation"
                          />
                        </li>
                      </ul>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import QueryEMX2 from "../../../molgenis-components/src/queryEmx2/queryEmx2";
// @ts-ignore
import { Breadcrumb } from "molgenis-components";
import { IBiobanks, IServices } from "../interfaces/directory";
import { useSettingsStore } from "../stores/settingsStore";
import { useRoute } from "vue-router";
import ReportTitle from "../components/report-components/ReportTitle.vue";
import CheckOut from "../components/checkout-components/CheckOut.vue";
import string from "../components/generators/view-components/string.vue";
import Quality from "../components/generators/view-components/quality.vue";
import ContactInformation from "../components/report-components/ContactInformation.vue";
import Button from "../components/Button.vue";
import { useCheckoutStore } from "../stores/checkoutStore";
import useErrorHandler from "../composables/errorHandler";

const service = ref<IServices | null>(null);
const { setError } = useErrorHandler();

new QueryEMX2(useSettingsStore().config.graphqlEndpoint)
  .table("Services")
  .select([
    "id",
    { biobank: ["id", "name"] },
    "name",
    { serviceTypes: ["name", "label", { serviceCategory: ["name"] }] },
    "acronym",
    "description",
    "descriptionUrl",
    "device",
    "deviceSystem",
    { tRL: ["order", "name", "label", "code", "definition"] },
    "accessDescriptionUrl",
    "unitOfAccess",
    "accessDescription",
    "unitCost",
    {
      qualityStandards: [
        "id",
        { qualityStandard: ["order", "name", "label", "code", "definition"] },
        { assessmentLevel: ["order", "name", "label", "code", "definition"] },
        "certificationNumber",
        "certificationReport",
        "certificationImageLink",
      ],
    },
    {
      contactInformation: [
        "id",
        "title_before_name",
        "first_name",
        "last_name",
        "title_after_name",
        "email",
        "phone",
        "address",
        "zip",
        "city",
        { country: ["order", "name", "label", "code", "definition"] },
        "role",
      ],
    },
    {
      national_node: [
        "id",
        "description",
        "dns",
        { data_refresh: ["order", "name", "label", "code", "definition"] },
        "date_start",
        "date_end",
      ],
    },
  ])
  .where("id")
  .equals(useRoute().params.id)
  .execute()
  .then((data: any) => {
    if (data.Services?.length) {
      service.value = data.Services[0];
    } else {
      setError("Service not found");
    }
  })
  .catch((error) => {
    setError(error);
  });

const qualityProps = computed(() => {
  return {
    label: "Quality labels",
    value: service.value?.qualityStandards?.map((quality) => {
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
  useCheckoutStore().isInCart(service.value?.id ?? "")
);

function toggleSelection() {
  if (!service.value) {
    throw new Error("Service is not loaded yet");
  }
  const bookmark = false;

  if (isInCart.value) {
    useCheckoutStore().removeServicesFromSelection(
      { name: service.value.biobank.name },
      [service.value.id],
      bookmark
    );
  } else {
    useCheckoutStore().addServicesToSelection(
      {
        id: service.value.biobank.id,
        name: service.value.biobank.name,
      } as IBiobanks,
      [{ label: service.value.name, value: service.value.id }],
      bookmark
    );
  }
}
</script>
