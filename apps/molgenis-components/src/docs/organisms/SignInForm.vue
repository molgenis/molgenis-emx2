<template>
  <demo-item id="input-string" label="Signin form">
    <div>
      <SignInForm
        :show="isShown"
        @cancel="isShown = false"
        @requestSignIn="handleSignInRequest(...arguments)"
      />
    </div>
    <div><button type="button" class="btn" @click="isShown = true">Show</button></div>
  </demo-item>
</template>
<script>
import SignInForm from '../../components/organisms/SignInForm.vue';
export default {
  components: {SignInForm},
  data: function () {
    return {
      isShown: false
    };
  },
  methods: {
    handleSignInRequest({email, password}) {
      alert(`handleSignInRequest, email = ${email}, pw = ${password}`);
    }
  }
};
</script>