<template>
  <div>
    <h3>Privacy policy</h3>
    <div class="btn-group" role="group" aria-label="Basic example">
      <button
        type="button"
        class="btn btn-secondary"
        :onClick="setPrefabPolicy('Policy1')"
      >
        Policy level 1
      </button>
      <button
        type="button"
        class="btn btn-secondary"
        :onClick="setPrefabPolicy('Policy2')"
      >
        Policy level 2
      </button>
      <button
        type="button"
        class="btn btn-secondary"
        :onClick="setPrefabPolicy('Policy3')"
      >
        Policy level 3
      </button>
      <button
        type="button"
        class="btn btn-secondary"
        :onClick="setPrefabPolicy('Policy4')"
      >
        Policy level 4
      </button>
    </div>
    <InputText
      id="privacy-policy-text"
      v-model="privacyPolicy"
      label="label"
      placeholder="Enter the privacy policy"
      description="This should have default value?"
    />
    <button type="button" class="btn btn-primary" :onClick="save">Save</button>
  </div>
</template>

<script>
const prefabPrivacyPolicies = {
  Policy1: "Policy 1",
  Policy2: "Policy 2",
  Policy3: "Policy 3",
  Policy4: "Policy 4",
};
export default {
  name: "ManageSettings",
  data() {
    return {
      privacyPolicy: "",
    };
  },
  methods: {
    setPrefabPolicy(index) {
      this.privacyPolicy = prefabPrivacyPolicies[index];
    },
    save() {
      console.log(this.privacyPolicy);
    },
  },
};
</script>
