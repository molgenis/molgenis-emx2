from setuptools import setup

setup(
    name='molgenis-py-client',
    version='2.4.1',
    description='The Python client for the MOLGENIS REST API',
    url='https://github.com/molgenis/molgenis-py-client/',
    license='GNU Lesser General Public License 3.0',
    packages=['molgenis'],
    python_requires='>=3.6',
    install_requires=['requests>=2.21.0'],
    test_suite='nose.collector',
    tests_require=['nose']
)
