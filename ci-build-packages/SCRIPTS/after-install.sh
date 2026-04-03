
chown molgenis /usr/local/share/molgenis -R
chown molgenis /var/log/molgenis -R
systemctl enable molgenis-emx2
systemctl start molgenis-emx2
