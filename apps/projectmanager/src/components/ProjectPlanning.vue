<template>
  <div>
    <h1>Project planning:</h1>
    <div class="row bg-white">
      <div class="col-1"><b>Project</b></div>
      <div class="col-11">
        <div class="row">
          <div class="col-1"><b>Unit</b></div>
          <div class="col-1"><b>PM</b></div>
          <div class="col-1"><b>booked</b></div>
          <div class="col-1"><b>remain</b></div>
          <div class="col-7"><b>plan</b></div>
        </div>
      </div>
    </div>
    <template v-if="rows">
      <div
        v-for="project in rows.filter((row) => !row.completed)"
        class="border border-black row bg-white"
      >
        <div class="col-12">
          <div class="row">
            <div class="col">
              <h5>{{ project.name }}</h5>
            </div>
          </div>
          <div class="row">
            <div class="col-1">
              <div class="ml-2">start: {{ project.startDate }}</div>
              <div class="ml-2">end: {{ project.endDate }}</div>
            </div>
            <div class="col-11">
              <div v-for="unit in project.projectUnits" class="row">
                <div class="col-1">{{ unit.unit }}</div>
                <div class="col-1">{{ (unit.planHours / 133).toFixed(1) }}</div>
                <div class="col-1">
                  {{
                    unit.panama
                      ? (unit.panama[0].regHours / 133).toFixed(1)
                      : ""
                  }}
                </div>
                <div class="col-1">
                  {{
                    unit.panama
                      ? Math.max(
                          0,
                          (
                            (unit.planHours - unit.panama[0].regHours) /
                            133
                          ).toFixed(1)
                        )
                      : ""
                  }}
                </div>
                <div class="col-1">
                  {{
                    unit.planning
                      ? unit.planning
                          .reduce(
                            (accum, curr) => accum + planPm(curr, project),
                            0
                          )
                          .toFixed(1)
                      : 0
                  }}PM
                </div>
                <div class="col-7">
                  <template v-for="planning in unit.planning"
                    ><div v-if="planPm(planning, project) > 0">
                      {{ planPm(planning, project).toFixed(1) }}PM:
                      {{ planning.person.name }} ({{ planning.fTE }}fte from
                      {{ planning.startDate }} until {{ planning.endDate }})
                      <RowButtonEdit
                        :id="JSON.stringify(planning) + 'edit'"
                        tableId="Planning"
                        tableLabel="Planning"
                        :pkey="planning"
                        @close="reload"
                      />
                      <RowButtonDelete
                        :id="JSON.stringify(planning) + 'delete'"
                        tableId="Planning"
                        tableLabel="Planning"
                        :pkey="planning"
                        @success="reload"
                      />
                      <RowButtonAdd
                        :id="JSON.stringify(unit) + 'add'"
                        tableId="Planning"
                        tableLabel="Planning"
                        :defaultValue="{
                          startDate: new Date(),
                          projectUnit: {
                            project: { name: project.name },
                            unit: unit.unit,
                          },
                        }"
                        @close="reload"
                      />
                    </div>
                  </template>
                  <RowButtonAdd
                    v-if="!unit.planning"
                    :id="JSON.stringify(unit) + 'add'"
                    tableId="Planning"
                    tableLabel="Planning"
                    :defaultValue="{
                      startDate: new Date(),
                      projectUnit: {
                        project: { name: project.name },
                        unit: unit.unit,
                      },
                    }"
                    @close="reload"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-if="loading">loading...</div>
    <div v-else-if="graphqlError">Error: {{ graphqlError }}</div>
  </div>
</template>

<script setup>
import { request } from "graphql-request";
import { ref } from "vue";
import query from "../gql/projects";
import {
  RowButtonEdit,
  RowButtonAdd,
  RowButtonDelete,
} from "molgenis-components";

const rows = ref();
const loading = ref(true);
const graphqlError = ref(null);

function planPm(planning, project) {
  if (!planning.endDate) {
    planning.endDate = new Date();
  }
  if (new Date(planning.endDate) < new Date()) {
    return 0;
  }
  const startDate =
    new Date() < new Date(planning.startDate)
      ? new Date(planning.startDate)
      : new Date();
  const endDate =
    project.endDate && new Date(project.endDate) < new Date(planning.endDate)
      ? new Date(project.endDate)
      : new Date(planning.endDate);

  var approxMonth = (endDate - startDate) / ((24 * 60 * 60 * 1000 * 365) / 12); //millisconds per day * 30 days

  return approxMonth * planning.fTE;
}

function reload() {
  console.log("reload");
  request("graphql", query)
    .then((data) => {
      rows.value = data["Projects"];
      loading.value = false;
    })
    .catch((error) => {
      if (Array.isArray(error.response.errors)) {
        graphqlError.value = error.response.errors[0].message;
      } else {
        graphqlError.value = error;
      }
      loading.value = false;
    });
}

reload();
</script>
