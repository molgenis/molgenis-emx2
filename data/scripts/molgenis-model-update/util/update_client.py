from molgenis_emx2_pyclient import Client
from zip_handling import Zip

import os
import shutil


class UpdateClient(Client):
    """
    An extended molgenis-emx2-pyclient session.
    Adds extra functions for updating data models
    """

    def __init__(self, url, token, schema=None, module=None):
        super().__init__(url, schema, token)

        self.update_module = module

    def re_create_model(self, database, database_type):
        if database in self.schema_names:
            # Extract data from the schema
            self.save_data(database)
            self.transform_data(database, database_type)
            print(f" 🔄 Recreate {database}")
            description = [s.description for s in self.schemas if s.name == database][0]
            self.recreate_schema(name=database, description=description,
                                 template=database_type, include_demo_data=False)
            print(f" ⬆️ Upload data into {database}")
            self.upload_zip(database, f"{database}_upload.zip")

        else:
            print(f" ⚙️ Create {database}")
            self.create_schema(name=database, description=database,
                               template=database_type, include_demo_data=False)

    def save_data(self, database):
        print(f" 🗄️ Extract current data from {database} into {database}_save.zip")
        self.export(schema=database)
        os.rename(f"{database}.zip", f"{database}_save.zip")
        shutil.copy(f"{database}_save.zip", f"{database}_data.zip")

    def transform_data(self, database: str, database_type: str):
        # Transform data from the ERIC schema
        print(f" ✏️ Transform {database} data")
        # Get instances of classes
        zip_handling = Zip(database)
        update = self.update_module.Transform(database, database_type)
        # run zip and transform functions
        zip_handling.unzip_data()
        update.delete_data_model_file()  # delete molgenis.csv from data folder
        # update.update_meta_data()
        update.transform_data()
        zip_handling.zip_data()

    def upload_zip(self, database, file):
        """Upload molgenis zip to fill a database"""

        zip_file = {'file': open(file, 'rb')}

        response = self.session.post(
            f"{self.url}/{database}/api/zip?async=true",
            allow_redirects=True,
            headers={'x-molgenis-token': self.token},
            # 'Content-Type': 'multipart/form-data'},
            files=zip_file
        )

        zip_file['file'].close()

        if response.status_code == 200:
            print(f"    ✅ Imported data into {database}, "
                  f"job_id: {response.json()['id']}")
        else:
            errors = '\n'.join([err['message']
                                for err in response.json().get('errors')])
            print(f" ❌Failed to import data into {database}, {errors}")
