const IMAGE_EXTENSION = /\.(svg|png|jpg|jpeg|gif|webp|avif)$/i;

export function toLogoPath(logoFileName: string): string {
  const fileName = IMAGE_EXTENSION.test(logoFileName)
    ? logoFileName
    : `${logoFileName}.svg`;
  return `/_nuxt-styles/logos/${fileName}`;
}
