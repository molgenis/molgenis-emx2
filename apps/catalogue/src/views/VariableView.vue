<template>
  <div class="container bg-white">
    <ButtonAlt @click="toggleNA" class="float-right text-white">
      {{ hideNA ? "Show" : "Hide" }} empty fields (N/A)
    </ButtonAlt>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div v-if="variable">
      <div class="p-2 bg-dark text-white mb-3">
        <h6>
          <RouterLink to="/" class="text-white"> home</RouterLink>
          /
          <RouterLink to="Variables" class="text-white"> variables </RouterLink>
          /
        </h6>
      </div>
      <h6 class="d-inline">{{ resourceType.slice(0, -1) }}:&nbsp;</h6>
      <RouterLink
        :to="{
          name: resourceType + '-details',
          params: {
            id: variable.resource.id,
          },
        }"
        >{{ variable.resource.id }}
      </RouterLink>
      /
      <h6 class="d-inline">Dataset:</h6>
      <RouterLink
        :to="{
          name: 'Datasets-details',
          params: {
            id: variable.resource.id,
            name: variable.dataset.name,
          },
        }">
        {{ variable.dataset.name }}
      </RouterLink>

      <h1>Variable: {{ variable.name }}</h1>
      <div class="row">
        <div class="col">
          <h6>Variable name</h6>
          <p>{{ variable.name }}</p>
          <span v-if="variable.description || !hideNA">
            <h6>Variable Description</h6>
            <p>{{ variable.description ? variable.description : "N/A" }}</p>
          </span>
          <span v-if="variable.format || !hideNA">
            <h6>Variable format</h6>
            <OntologyTerms :terms="[variable.format]" color="dark" />
          </span>
          <span v-if="variable.keywords || !hideNA">
            <h6>Variable keywords</h6>
            <OntologyTerms :terms="variable.keywords" color="dark" />
          </span>
          <span v-if="variable.unit || !hideNA">
            <h6>Variable unit</h6>
            <OntologyTerms :terms="[variable.unit]" color="dark" />
          </span>
          <span v-if="variable.mandatory || !hideNA">
            <h6>Mandatory</h6>
            <p>{{ variable.mandatory ? "YES" : "NO" }}</p>
          </span>
          <span v-if="variable.vocabularies || !hideNA">
            <h6>Vocabularies</h6>
            <OntologyTerms :terms="variable.vocabularies" color="dark" />
          </span>
          <span v-if="variable.permittedValues || !hideNA">
            <h6>Permitted values</h6>
            <table
              v-if="variable.permittedValues"
              class="table table-sm table-bordered m-2">
              <thead>
                <th>value</th>
                <th>label</th>
                <th>isMissing</th>
              </thead>
              <tbody>
                <tr v-for="v in variable.permittedValues">
                  <td>{{ v.value }}</td>
                  <td>{{ v.label }}</td>
                  <td>
                    {{ v.isMissing ? "YES" : "NO" }}
                  </td>
                </tr>
              </tbody>
            </table>
            <p v-else>N/A</p>
          </span>
          <span v-if="variable.exampleValues || !hideNA">
            <h6>Variable Example values</h6>
            <ul v-if="variable.exampleValues">
              <li v-for="v in variable.exampleValues" :key="v">'{{ v }}'</li>
            </ul>
            <p v-else>N/A</p>
          </span>
          <span v-if="variable.repeats || !hideNA">
            <h6>This variable is repeated as:</h6>
            <table
              v-if="variable.repeats"
              class="table table-sm table-bordered m-2">
              <thead>
                <th>table</th>
                <th>name</th>
                <th>collectionEvent</th>
              </thead>
              <tbody>
                <tr v-for="v in variable.repeats">
                  <td>{{ v.table.name }}</td>
                  <td>{{ v.name }}</td>
                  <td>
                    {{ v.collectionEvent ? v.collectionEvent.name : "N/A" }}
                  </td>
                </tr>
              </tbody>
            </table>
            <p v-else>N/A</p>
          </span>
          <span v-if="variable.notes || !hideNA">
            <h6>Variable Notes</h6>
            <p>{{ variable.notes ? variable.notes : "N/A" }}</p>
          </span>
        </div>
        <div class="col"></div>
      </div>
      <span v-if="variable.mappings || !hideNA">
        <h6>Mappings</h6>
        <table v-if="variable.mappings" class="table table-sm table-bordered">
          <thead>
            <tr>
              <th>source</th>
              <th>sourceVariables</th>
              <th>match</th>
              <th>description</th>
              <th>syntax</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in variable.mappings">
              <td>
                <RouterLink
                  :to="{
                    name: 'DatasetMappings-details',
                    params: {
                      source: m.source.id,
                      sourceDataset: m.sourceDataset.name,
                      target: variable.resource.id,
                      targetDataset: variable.dataset.name,
                    },
                  }">
                  {{ getType(m.source.mg_tableclass) }}:
                  {{ m.source.id }}
                  Dataset:
                  {{ m.sourceDataset.name }}
                </RouterLink>
              </td>

              <td>
                <div v-for="v in m.sourceVariables" :key="v.name">
                  <RouterLink
                    :to="{
                      name: 'Variables-details',
                      params: {
                        resource: v.resource.id,
                        dataset: v.dataset.name,
                        name: v.name,
                      },
                    }"
                    >{{ v.name }}
                  </RouterLink>
                </div>
              </td>
              <td>{{ m.match ? m.match.name : "" }}</td>
              <td>{{ m.description }}</td>
              <td>
                <pre>{{ m.syntax }}</pre>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-else>N/A</p>
      </span>
    </div>
  </div>
</template>

<script>
import {
  MessageError,
  ButtonAlt,
  convertToPascalCase,
} from "molgenis-components";
import { request, gql } from "graphql-request";
import OntologyTerms from "../components/OntologyTerms.vue";

export default {
  components: {
    OntologyTerms,
    MessageError,
    ButtonAlt,
  },
  props: {
    resource: String,
    dataset: String,
    name: String,
  },
  data() {
    return {
      graphqlError: null,
      variable: null,
      hideNA: true,
    };
  },
  computed: {
    resourceType() {
      if (this.variable.resource) {
        return this.getType(this.variable.resource.mg_tableclass);
      }
    },
  },
  methods: {
    toggleNA() {
      this.hideNA = !this.hideNA;
    },
    getType(mg_tableclass) {
      return convertToPascalCase(mg_tableclass.split(".")[1]);
    },
    reload() {
      request(
        "graphql",
        gql`
          query Variables($resource: String, $dataset: String, $name: String) {
            Variables(
              filter: {
                resource: { id: { equals: [$resource] } }
                dataset: { name: { equals: [$dataset] } }
                name: { equals: [$name] }
              }
            ) {
              name
              resource {
                id
                name
                mg_tableclass
              }
              dataset {
                name
                resource {
                  id
                }
              }
              unit {
                name
                definition
                ontologyTermURI
              }
              format {
                name
                definition
                ontologyTermURI
              }
              repeats {
                name
                dataset {
                  name
                }
              }
              collectionEvent {
                name
              }
              vocabularies {
                name
                definition
                ontologyTermURI
              }
              label
              notes
              mandatory
              description
              exampleValues
              permittedValues {
                isMissing
                value
                label
              }
              keywords {
                name
                definition
                ontologyTermURI
              }
              mappings {
                description
                syntax
                match {
                  name
                }
                source {
                  id
                  name
                  mg_tableclass
                }
                target {
                  id
                }
                sourceDataset {
                  name
                }
                sourceVariables {
                  name
                  dataset {
                    name
                  }
                  resource {
                    id
                  }
                }
              }
              sinceVersion
            }
          }
        `,
        {
          resource: this.resource,
          dataset: this.dataset,
          name: this.name,
        }
      )
        .then(data => {
          this.variable = data.Variables[0];
        })
        .catch(error => {
          if (error.response)
            this.graphqlError = error.response.errors[0].message;
          else this.graphqlError = error;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.reload();
  },
};
</script>
