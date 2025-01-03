export default defineNuxtRouteMiddleware(async (to, from) => {
  const {data: session} = await useSession();
  const isAdmin = computed(() => session.value.email === "admin");
    if (!isAdmin.value) {
      return navigateTo('/login')
    }
  })