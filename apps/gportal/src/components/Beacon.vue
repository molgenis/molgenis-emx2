<template>
  <div>
    <h1>Beacon</h1>
    <label>Choose Gene</label>
    <InputRefList
      tableName="Genes"
      v-model="genes"
      refLabel="${name}"
      :multi-select="true"
    />
    <div>
      <ButtonAction @click="queryBeacon">Search</ButtonAction>
      <h2>Results</h2>
    </div>
    Beacon output:
    <pre>
      {{ beaconOutput }}
    </pre>
    <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>
  </div>
</template>

<script>
import {
  ButtonAction,
  InputRefList,
  ShowMore,
  MessageError,
} from "molgenis-components";
export default {
  components: {
    ButtonAction,
    InputRefList,
    ShowMore,
    MessageError,
  },
  data() {
    return {
      genes: [],
      beaconOutput: null,
      errorMessage: null,
    };
  },
  methods: {
    async queryBeacon() {
      console.log("fetching");
      fetch("/api/beacon")
        .then((response) => response.json())
        .then((json) => (this.beaconOutput = json))
        .catch((error) => (this.errorMessage = error));
    },
  },
};
</script>
