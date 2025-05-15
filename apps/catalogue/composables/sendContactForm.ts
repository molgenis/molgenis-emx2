import { useRoute } from "#imports";
import type { IContactFormData } from "~/interfaces/types";

export const sendContactForm = (formData: IContactFormData) => {
  const route = useRoute();
  return $fetch(`/api/message/`, {
    method: "POST",
    body: formData,
  });
};
