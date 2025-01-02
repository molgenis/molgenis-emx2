export default defineNuxtPlugin(() => {
    if (process.client) {
        sessionStorage.clear();
        console.log('Session storage cleared on app start');
    }
});