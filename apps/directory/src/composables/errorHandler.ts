import { ref } from "vue";

const error = ref<null | any>(null);
export default function useErrorHandler() {
  const setError = (newError: any) => {
    console.error("An error occurred: ", newError);
    error.value = newError;
  };

  const clearError = () => {
    error.value = null;
  };

  return { error, setError, clearError };
}
