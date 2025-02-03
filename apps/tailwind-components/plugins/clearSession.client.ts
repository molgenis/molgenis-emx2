export default defineNuxtPlugin(() => {
    if (import.meta.client) {
        sessionStorage.clear();
        console.log('Session storage cleared on app start. Removed caches of getSchema');
    }
});