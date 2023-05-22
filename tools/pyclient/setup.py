from setuptools import setup

setup(
    name='molgenis-py-client',
    version='0.1.0',
    description='Python client for the MOLGENIS EMX2 API',
    url='https://github.com/molgenis/molgenis-emx2/',
    license='GNU Lesser General Public License 3.0',
    packages=['molgenis'],
    python_requires='>=3.6',
    install_requires=['requests>=2.27.1'],
    test_suite='nose.collector',
    tests_require=['nose']
)
