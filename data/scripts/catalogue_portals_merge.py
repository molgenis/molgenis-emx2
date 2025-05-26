"""Script for transformations"""

import logging
from pathlib import Path, PosixPath
import pandas as pd


DATA_DIR = Path(__file__).parent.parent.absolute()

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger("post-merge transformations")


def recode_
