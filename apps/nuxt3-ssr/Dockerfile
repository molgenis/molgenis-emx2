####
## Build file for server side rendered (ssr) catalogue application for use with molgenis EMX2 backend
###

## Base image to have a node runtime
FROM node:19.8.1-alpine

# Used to build theme
# ENV EMX2_THEME=$EMX2_THEME

WORKDIR /

## Copy the files need from the contaxt into to image
COPY ./.nuxt /.nuxt
COPY ./.output /.output

# Expose $PORT on container.
# We use a varibale here as the port is something that can differ on the environment.
EXPOSE $PORT

# Set host to localhost / the docker image
ENV NUXT_HOST=0.0.0.0

# Set app port
ENV NUXT_PORT=$PORT

# Set the base url
ENV NUXT_PUBLIC_API_BASE=$NUXT_PUBLIC_API_BASE

# Set the theme name
ENV NUXT_PUBLIC_EMX2_THEME=$NUXT_PUBLIC_EMX2_THEME

# Set the logo name 
ENV NUXT_PUBLIC_EMX2_LOGO=$NUXT_PUBLIC_EMX2_LOGO

# Set the site title
ENV NUXT_PUBLIC_SITE_TITLE=$NUXT_PUBLIC_SITE_TITLE

## Start the server
CMD node .output/server/index.mjs

# To run on the vm, for example using the host network ( and fill in server-location, container-name and image)
# docker run -d --network host --env NUXT_PUBLIC_API_BASE=[server-location] --name [container-name] [image]


