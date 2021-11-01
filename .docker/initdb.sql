/* use this init script if you want to test EMX2 with non-molgenis user */
CREATE DATABASE molgenisdb;
CREATE USER molgenis_admin WITH LOGIN NOSUPERUSER INHERIT CREATEROLE ENCRYPTED PASSWORD 'molgenis_admin';
GRANT ALL PRIVILEGES ON DATABASE molgenisdb TO molgenis_admin;
