import type { IContactFormData } from "~/interfaces/types";

export const sendContactForm = (formData: IContactFormData) => {
  const route = useRoute();
  return $fetch(`/${route.params.schema}/api/message/`, {
    method: "POST",
    body: formData,
  });
};
