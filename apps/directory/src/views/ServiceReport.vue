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
          <ReportTitle type="Service" :name="service.name" />
          <ReportDescription :description="service.description" />

          <div class="container p-0">
            <div class="row">
              <div class="col-md-8">
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
                      :attribute="service.qualityStandards"
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

      <pre>{{ service }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import QueryEMX2 from "../../../molgenis-components/src/queryEmx2/queryEmx2";
// @ts-ignore
import { Breadcrumb } from "molgenis-components";
import { IServices } from "../interfaces/directory";
import { useSettingsStore } from "../stores/settingsStore";
import { useRoute } from "vue-router";
import ReportTitle from "../components/report-components/ReportTitle.vue";
import ReportDescription from "../components/report-components/ReportDescription.vue";
import CheckOut from "../components/checkout-components/CheckOut.vue";
import string from "../components/generators/view-components/string.vue";
import Quality from "../components/generators/view-components/quality.vue";
import ContactInformation from "../components/report-components/ContactInformation.vue";

const service = ref<IServices | null>(null);

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
  .then((data) => {
    // @ts-ignore
    service.value = data.Services[0];
  });
</script>
