/* use this init script if you want to test EMX2 with non-molgenis user */
CREATE DATABASE molgenis_cloud;
CREATE USER molgenis_cloud WITH LOGIN NOSUPERUSER INHERIT CREATEROLE ENCRYPTED PASSWORD 'molgenis_cloud';
GRANT ALL PRIVILEGES ON DATABASE molgenis_cloud TO molgenis_cloud;

