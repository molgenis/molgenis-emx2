<template>
  <div>
    <h1>Person planning:</h1>
    <table class="table-bordered w-100 bg-white">
      <thead>
        <th class="align-top">Person</th>
        <th class="align-top">FTE</th>
        <th>
          Planning:
          <div class="row">
            <div class="col-1">FTE</div>
            <div class="col-4">projectUnit</div>
            <div class="col-5">period</div>
          </div>
        </th>
      </thead>

      <tr v-for="person in rows" class="bg-white">
        <td class="align-top">{{ person.name }}</td>
        <td class="align-top">{{ person.fTE }}</td>
        <td>
          <RowButtonAdd
            v-if="!person.planning"
            tableId="Planning"
            :defaultValue="{ person: { name: person.name } }"
            @close="reload"
          />
          <div class="row" v-for="planning in person.planning">
            <div class="col-1">{{ planning.fTE }}</div>
            <div class="col-4">
              {{ planning.projectUnit.project.name }}:{{
                planning.projectUnit.unit
              }}
            </div>
            <div class="col-5">
              {{ planning.startDate }} until {{ planning.endDate }}
              <RowButtonEdit
                tableId="Planning"
                tableLabel="Planning"
                :pkey="planning"
                @close="reload"
              />
              <RowButtonDelete
                tableId="Planning"
                tableLabel="Planning"
                :pkey="planning"
                @success="reload"
              />
              <RowButtonAdd
                tableId="Planning"
                :defaultValue="{ person: { name: person.name } }"
                @close="reload"
              />
            </div>
          </div>
        </td>
      </tr>
    </table>
    <div v-if="loading">loading...</div>
    <div v-else-if="graphqlError">Error: {{ graphqlError }}</div>
  </div>
</template>

<script setup>
import { request } from "graphql-request";
import { ref } from "vue";
import query from "../gql/persons";
import {
  RowButtonEdit,
  RowButtonAdd,
  RowButtonDelete,
} from "molgenis-components";

const rows = ref();
const loading = ref(true);
const graphqlError = ref(null);

function reload() {
  console.log("reload");
  request("graphql", query)
    .then((data) => {
      rows.value = data["Persons"];
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
