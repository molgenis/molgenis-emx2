import type { IContactFormData } from "~/interfaces/types";

export const sendContactForm = (formData: IContactFormData) => {
  const route = useRoute();
  const config = useRuntimeConfig();
  return $fetch(`/${route.params.schema}/api/message/`, {
    method: "POST",
    baseURL: config.public.apiBase,
    body: formData,
  });
};
