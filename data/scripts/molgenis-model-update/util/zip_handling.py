import os
from zipfile import ZipFile
import shutil
import logging
import stat


class Zip:
    """Functions to zip and unzip data
    """

    def __init__(self, database):
        self.database = database
        self.path = self.database + '_data'
        self.logger = logging.getLogger(' data update and transform')

    def remove_unzipped_data(self):
        """Remove extracted unzipped data from previous run of script
        """
        try:
            # remove unzipped data and avoid Windows PermissionError
            shutil.rmtree(self.path, onerror=lambda func, path, _: (os.chmod(path, stat.S_IWRITE), func(path)))
        except FileNotFoundError:
            self.logger.info('No unzipped data was found')

    def unzip_data(self):
        """Extract data.zip
        """
        data = ZipFile(self.path + '.zip')
        try:
            data.extractall(self.path)
        except FileNotFoundError:
            self.logger.error('unzip failed')
            exit()
        except PermissionError:
            self.logger.error('Error: unzip failed, permission denied')
            exit()
        try:
            if os.path.exists(self.database + '_data.zip'):
                os.remove(self.database + '_data.zip')
        except PermissionError:
            # remove fails on windows, is not needed on Windows, pass
            self.logger.warning('Warning: Error deleting ' + self.database + '_data.zip')

    def zip_data(self):
        """Zip transformed data to upload.zip
        """
        shutil.make_archive(self.database + '_upload', 'zip', self.path)
