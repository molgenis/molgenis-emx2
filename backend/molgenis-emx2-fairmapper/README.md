# FAIRmapper Module

Gradle module for loading and testing FAIR mapping bundles.

## Structure

- `BundleLoader.java` - Loads mapping.yaml files into MappingBundle objects
- `JsltTransformEngine.java` - Applies JSLT transformations to JSON
- `MappingBundle.java` - Root model with metadata and endpoints
- `Endpoint.java` - API endpoint with processing steps
- `Step.java` - Single transformation or query step
- `TestCase.java` - Input/output test case pair

## Testing

`BeaconBundleTest.java` loads the beacon-v2 bundle and validates all JSLT transforms against test cases.

To test manually:
1. Fix .git file to use absolute path
2. Run: `./gradlew :backend:molgenis-emx2-fairmapper:test`
