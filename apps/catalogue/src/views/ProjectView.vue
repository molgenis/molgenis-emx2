<template>
  <div>
    <h1>
      <small>Project:</small><br />{{ project.name }} ({{ project.acronym }})
    </h1>
    <label>Description:</label>
    <ReadMore
      :text="project.description"
      :length="1000"
      v-if="project.description"
    />
    <h4>Tables:</h4>
    <TableList :projectAcronym="project.acronym" />
    <VariableTree :resourceAcronym="project.acronym" />
  </div>
</template>

<script>
import { request } from "graphql-request";
import { MessageError, ReadMore } from "@mswertz/emx2-styleguide";
import TableList from "../components/TableList";
import VariableTree from "../components/VariableTree";

export default {
  components: {
    VariableTree,
    MessageError,
    ReadMore,
    TableList,
  },
  props: {
    projectAcronym: String,
  },
  data() {
    return {
      error: null,
      project: {},
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Projects($acronym:String){Projects(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},institution{acronym,name}, description,website, investigators{name}, supplementaryInformation, tables{name}}}`,
        {
          acronym: this.projectAcronym,
        }
      )
        .then((data) => {
          this.project = data.Projects[0];
        })
        .catch((error) => {
          this.error = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.reload();
  },
  watch: {
    projectAcronym() {
      this.reload();
    },
  },
};
</script>
