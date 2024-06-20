import glob
import os
import pandas as pd
import shutil
# import sys
# sys.path.insert(1, "/".join(os.path.realpath(__file__).split("/")[0:-2]) + "/util")
from util.update_client import UpdateClient  # noqa: E402
from util.zip_handling import Zip  # noqa: E402


class NnIdValidations:
    """General function to add ID validation expressions to the Directory staging areas
    """

    def __init__(self, database_name, database_type, update_session: UpdateClient):
        self.database = database_name
        self.database_type = database_type
        self.path = self.database + '_data/'
        self.session = update_session
        self.meta_file = f"{self.path}molgenis.csv"
        self.zip_handling = Zip(self.database)
        extra_char = ""
        if len(self.database) == 3:
            extra_char = f"[{self.database[2]}]"
        self.validations = {
            "AlsoKnownIn":
                f"if(!/^bbmri-eric:akiID:{self.database}_[\\w-:]+$/.test(id))"
                f"'ID should start with bbmri-eric:akiID:{self.database}_'",
            "Biobanks": f"if(!/^bbmri-eric:ID:{self.database}_[\\w-:]+$/.test(id))"
                        f"'ID should start with bbmri-eric:ID:{self.database}_'",
            "Collections": f"if(!/^bbmri-eric:ID:{self.database}_[\\w-]+:collection:"
                           f"[\\w-:]+$/.test(id))"
                           f"'ID should start the ID of the biobank in which the "
                           f"collection resides + :collection:'",
            "CollectionFacts":
                f"if(!/^bbmri-eric:factID:{self.database}_[\\w-:]+$/.test(id))"
                f"'ID should start with bbmri-eric:factID:{self.database}_'",
            "Networks": f"if(!/^bbmri-eric:networkID:[{self.database[0]}E]"
                        f"[{self.database[1]}U]{extra_char}_[\\w-:]+$/.test(id))"
                        f"'ID should start with bbmri-eric:networkID:{self.database}_ "
                        f"or bbmri-eric:networkID:EU_'",
            "Persons": f"if(!/^bbmri-eric:contactID:[{self.database[0]}E]"
                       f"[{self.database[1]}U]{extra_char}_[\\w-:]+$/.test(id))"
                       f"'ID should start with bbmri-eric:contactID:{self.database}_ "
                       f"or bbmri-eric:contactID:EU_'",
                            }

    def add_validations(self):
        if self.database_type != "BIOBANK_DIRECTORY_STAGING":
            print("No ID validations need to be added")
            pass
        print(f"\nAdd ID validations to {self.database}")
        try:
            shutil.copy(f"{self.database}_save.zip", f"{self.database}_save_old.zip")
        except FileNotFoundError:
            print(f" ⚠️ {self.database}_"
                  f"save.zip not found, true in case of a new database")
        self.session.save_data(self.database)
        self.zip_handling.unzip_data()
        self.remove_files()
        df_meta = pd.read_csv(self.meta_file, sep=",")
        df_meta["validation"] = (
            df_meta.apply(lambda row:
                          self.add_expression(row.tableName, row.columnName), axis=1))
        df_meta = self.session.float_to_int(df_meta)
        df_meta.to_csv(f"{self.path}molgenis.csv", index=False)
        self.zip_handling.zip_data()
        print(f" ⬆️ Update {self.database} data model")
        self.session.upload_zip(self.database, f"{self.database}_upload.zip")

    def add_expression(self, table, column):
        if column == "id":
            return self.validations[table]

    def remove_files(self):
        """Delete all files except molgenis.csv"""
        for file in glob.glob(f"{self.path}/*.csv"):
            if not file.endswith(self.meta_file):
                os.remove(file)
