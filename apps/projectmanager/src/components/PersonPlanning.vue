<template>
  <div>
    <h1>Person planning:</h1>
    <table class="planning-table">
      <thead>
        <colgroup span="4"></colgroup>
        <colgroup span="5"></colgroup>
        <tr>
          <th colspan="4" scope="colgroup">Person Info</th>
          <th colspan="5" scope="colgroup">Planning</th>
        </tr>
        <tr>
          <th scope="col" data-col-name="Name">Name</th>
          <th scope="col" data-col-name="FTE">FTE</th>
          <th scope="col" data-col-name="Notes">Notes</th>
          <th scope="col" data-col-name="options">
            <span class="sr-only">options</span>
          </th>
          <th scope="col" data-col-name="FTE">FTE</th>
          <th scope="col" data-col-name="projectUnit">Project Units</th>
          <th scope="col" data-col-name="period">Period</th>
          <th scope="col" data-col-name="notes">Notes</th>
          <th scope="col" data-col-name="options">
            <span class="sr-only">options</span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="person in rows">
          <th class="align-top" data-col-name="Name">{{ person.name }}</th>
          <td class="align-top" data-col-name="FTE">{{ person.fTE }}</td>
          <td class="align-top" data-col-name="Notes">{{ person.notes }}</td>
          <td class="align-top" data-col-name="options">
            <div class="row-options">
              <RowButtonEdit
                :id="`person-info-${person.name}`"
                tableId="Persons"
                tableLabel="Persons"
                :pkey="person"
                @close="reload"
              />
              <RowButtonDelete
                :id="`person-info-${person.name}`"
                tableId="Persons"
                tableLabel="Persons"
                :pkey="person"
                @success="reload"
              />
              <RowButtonAdd
                :id="`person-info-${person.name}`"
                tableId="Persons"
                :defaultValue="{ person: { name: person.name } }"
                @close="reload"
              />
            </div>
          </td>
          <td class="align-top" data-col-name="FTE">
            <span v-for="planning in person.planning">{{ planning.fTE }}</span>
          </td>
          <td class="align-top" data-col-name="projectUnit">
            <div v-for="planning in person.planning">
              <span>
                {{ planning.projectUnit.project.name }}:
                {{ planning.projectUnit.unit }}
              </span>
            </div>
          </td>
          <td class="align-top" data-col-name="period">
            <div v-for="planning in person.planning">
              <span>{{ planning.startDate }} until {{ planning.endDate }}</span>
            </div>
          </td>
          <td class="align-top" data-col-name="notes">
            <div v-for="planning in person.planning">
              <span>{{ planning.notes }}</span>
            </div>
          </td>
          <td class="align-top" data-col-name="options">
            <template v-for="planning in person.planning">
              <div class="row-options">
                <RowButtonEdit
                  :id="`person-planning-${person.name}-${planning.name}`"
                  tableId="Planning"
                  tableLabel="Planning"
                  :pkey="planning"
                  @close="reload"
                />
                <RowButtonDelete
                  :id="`person-planning-${person.name}-${planning.name}`"
                  tableId="Planning"
                  tableLabel="Planning"
                  :pkey="planning"
                  @success="reload"
                />
                <RowButtonAdd
                  :id="`person-planning-${person.name}-${planning.name}`"
                  tableId="Planning"
                  :defaultValue="{ person: { name: person.name } }"
                  @close="reload"
                />
              </div>
            </template>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-if="loading">loading...</div>
    <div v-else-if="graphqlError">Error: {{ graphqlError }}</div>
  </div>
</template>

<script setup lang="ts">
import { request } from "graphql-request";
import { ref } from "vue";
import query from "../gql/persons";
import {
  RowButtonEdit,
  RowButtonAdd,
  RowButtonDelete,
} from "molgenis-components";

const rows = ref<object[]>();
const loading = ref<boolean>(true);
const graphqlError = ref<Error | null>(null);

function reload() {
  request("graphql", query)
    .then((data: object[]) => {
      rows.value = data["Persons"];
      loading.value = false;
    })
    .catch((error: Error) => {
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

<style lang="scss">
$border-gray-300: 1px solid #adadad;
$border-gray-100: 1px solid #e4e4e4;

.planning-table {
  width: 100%;
  background-color: white;
  position: relative;
  border-collapse: separate;
  border-spacing: 0;

  td,
  th {
    padding: 0.5em 0.75em;
  }

  thead {
    position: sticky;
    top: 0;

    th,
    td {
      border-right: $border-gray-100;
      border-bottom: $border-gray-100;
      background-color: #ffffff;

      &:nth-child(4) {
        border-right: $border-gray-300;
      }

      &:last-child {
        border-radius: none;
      }
    }
    tr {
      &:last-child {
        th,
        td {
          border-bottom: $border-gray-300;
        }
      }
    }
  }

  tbody {
    tr {
      td,
      th {
        font-weight: 400;
        border-bottom: $border-gray-100;
        border-right: $border-gray-100;

        &:nth-child(4) {
          border-right: $border-gray-300;
        }
      }

      td {
        &[data-col-name="FTE"] {
          text-align: right;
        }

        &[data-col-name="options"] {
          .row-options {
            display: flex;
            justify-content: center;
            align-items: center;
          }
        }
      }
    }
  }
}
</style>
