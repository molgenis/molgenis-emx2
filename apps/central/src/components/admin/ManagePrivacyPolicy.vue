<template>
  <div>
    <h3>Privacy policy</h3>
    <div>
      <button
        type="button"
        class="btn btn-secondary mr-1 mb-1"
        @click="setPrefabPolicy('policy1')"
      >
        Policy level 1
      </button>
      <button
        type="button"
        class="btn btn-secondary mr-1 mb-1"
        @click="setPrefabPolicy('policy2')"
      >
        Policy level 2
      </button>
      <button
        type="button"
        class="btn btn-secondary mr-1 mb-1"
        @click="setPrefabPolicy('policy3')"
      >
        Policy level 3
      </button>
      <button
        type="button"
        class="btn btn-secondary mr-1 mb-1"
        @click="setPrefabPolicy('policy4')"
      >
        Policy level 4
      </button>
    </div>
    <InputText
      id="privacy-policy-text"
      v-model="privacyPolicy"
      placeholder="Enter the privacy policy"
    />
    <button type="button" class="btn btn-primary" @click="save">Save</button>
  </div>
</template>

<script>
const prefabPrivacyPolicies = {
  policy1: "Policy 1",
  policy2: "Policy 2",
  policy3: "Policy 3",
  policy4: "Policy 4",
};

import { request, gql } from "graphql-request";
import { InputText } from "molgenis-components";

export default {
  name: "ManagePrivacyPolicy",
  components: {
    InputText,
  },
  data() {
    return {
      privacyPolicy: undefined,
    };
  },
  methods: {
    setPrefabPolicy(policy) {
      this.privacyPolicy = prefabPrivacyPolicies[policy];
    },
    async fetchPrivacyPolicy() {
      const response = await request("graphql", `{_settings{key, value}}`);
      const policyData = response._settings.find(
        (item) => item.key === "PrivacyPolicy"
      );
      if (policyData === undefined) {
        this.setPrefabPolicy("policy1");
      } else {
        this.privacyPolicy = policyData.value;
      }
    },
    async save() {
      const createMutation = gql`
        mutation createSetting($key: String, $value: String) {
          createSetting(key: $key, value: $value) {
            message
          }
        }
      `;

      const variables = {
        key: "PrivacyPolicy",
        value: this.privacyPolicy,
      };

      await request("graphql", createMutation, variables).catch((e) => {
        console.error(e);
      });
    },
  },
  mounted() {
    this.fetchPrivacyPolicy();
  },
};
</script>
