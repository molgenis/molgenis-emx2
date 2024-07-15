export default defineNuxtRouteMiddleware((to, from) => {
    const session = useSession();
 
    const isAdmin = computed(() => session.value?.email === "admin");
    if (!isAdmin.value) {
      return navigateTo('/login')
    }
  })