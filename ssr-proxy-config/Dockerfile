FROM nginx:1.16.0

# Remove default configuration
RUN rm -rf /etc/nginx/conf.d/*.conf
RUN rm -rf /etc/nginx/sites-enabled/*.conf

# Copy the server configuration
COPY emx2.conf /etc/nginx/conf.d/