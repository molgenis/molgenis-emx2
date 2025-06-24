import type { IContactFormData } from "~/interfaces/types";
import { useRuntimeConfig } from "#app";

export const sendContactForm = (formData: IContactFormData) => {
  const config = useRuntimeConfig();
  const schema = config.public.schema;
  return $fetch(`/${schema}/api/message/`, {
    method: "POST",
    body: formData,
  });
};
