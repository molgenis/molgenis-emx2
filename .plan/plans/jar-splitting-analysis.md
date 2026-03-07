# JAR Splitting Analysis: Enabling Docker Layer Caching with Multi-Release Support

## Executive Summary

The molgenis-emx2 project uses a ~268MB shadow JAR containing all dependencies merged into a single monolithic archive. This defeats Docker layer caching—every code change requires pushing the entire JAR to the registry. The challenge is that **GraalVM Truffle's Multi-Release JARs (META-INF/versions/) only work when classes are loaded from JAR files, not exploded directories**.

This analysis evaluates 5 approaches for splitting the build while preserving Multi-Release JAR semantics and identifies the recommended path forward.

---

## Current State

### Build Pipeline
- **Root**: `build.gradle` applies `com.gradleup.shadow:8.3.9` plugin
- **Manifest**: Sets `Multi-Release: true` (required for Truffle)
- **Output**: Single `molgenis-emx2-{version}.jar` (~268MB)
- **Docker**: Single `COPY` layer (monolithic)

### Multi-Release JAR Dependencies
Only **1 dependency uses Multi-Release JARs**:
- `org.graalvm.truffle:truffle-api:25.0.1` has versioned classes:
  - `META-INF/versions/9/module-info.class`
  - `META-INF/versions/21/com/oracle/truffle/api/impl/JDKAccessor.class`
  - `META-INF/versions/21/module-info.class`

**Other GraalVM dependencies** (js-language, js-scriptengine, polyglot, regex) do **NOT** use Multi-Release.

### Docker Strategy (Current)
```
FROM eclipse-temurin:21-jre-noble
RUN apt-get update && apt-get install -y python3...
COPY build/libs/molgenis-emx2-*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Single JAR layer = no caching on code-only changes.

---

## The Multi-Release JAR Problem

When the JVM loads a Multi-Release JAR, it:
1. Reads `META-INF/MANIFEST.MF` → recognizes `Multi-Release: true`
2. If running Java 21 (our runtime), looks for `META-INF/versions/21/` classes first
3. Falls back to base classes if version-specific class not found

**If the JAR is exploded to a directory**:
```
app/
  com/oracle/truffle/api/impl/JDKAccessor.class (base)
  versions/21/com/oracle/truffle/api/impl/JDKAccessor.class (java 21)
```
The JVM's Multi-Release classloader **does not check `versions/` directories**—it only recognizes them inside JAR archives. Result: Always loads base classes, breaking Java 21-optimized Truffle behavior.

---

## Approach Evaluations

### Approach 1: Gradle `application` Plugin + `installDist`

**How it works**: Gradle's built-in `application` plugin generates a distribution with:
```
build/install/app/
  bin/app
  lib/
    dependency-1.jar
    dependency-2.jar
    app.jar (thin, just your classes)
```

Docker can layer:
```dockerfile
COPY --link build/install/app/lib/ /app/lib/
COPY --link build/install/app/app.jar /app/app.jar
```

**Multi-Release Support**: YES
- Each dependency JAR (including Truffle) retains its `META-INF/MANIFEST.MF` and `META-INF/versions/` directory
- JVM loads them as individual JAR files, not exploded directories
- Truffle Multi-Release resolution works correctly

**Build Changes**: MINIMAL
- Remove shadowJar plugin
- Enable application plugin (partially configured, only needs mainClass)
- Create custom Dockerfile task to reference `installDist` output

**Implementation Complexity**: LOW
- 5-10 lines of Gradle code
- Dockerfile changes: 3-4 lines
- CI pipeline: change JAR_FILE path from `build/libs/` to `build/install/app/lib/`

**Layer Caching Effectiveness**:
```
Layer 1: /app/lib/ (50 files, ~250MB) — changes only on dependency updates
Layer 2: /app/app.jar (~10MB) — changes every code commit
```
On code-only change: push ~10MB (vs. ~268MB today) = **96.3% reduction**

**Maintenance Burden**: VERY LOW
- Gradle's standard plugin, widely used, well-maintained
- No custom build logic
- No workarounds for Multi-Release semantics

**Limitations**:
- Requires classpath entry: `java -cp "/app/lib/*:/app/app.jar"` (or CLASSPATH env var)
- `installDist` creates scripts (bin/app) we don't use in Docker
- Slightly larger image due to individual JAR files vs. merged JAR (negligible: ~10MB overhead from compression inefficiency)

**Verification Path**:
```bash
./gradlew installDist
unzip -p build/install/app/lib/truffle-api-25.0.1.jar META-INF/MANIFEST.MF | grep Multi-Release
unzip -l build/install/app/lib/truffle-api-25.0.1.jar | grep META-INF/versions/
```

---

### Approach 2: Shadow JAR with Relocation/Split Configs

**How it works**: Configure shadowJar to produce multiple outputs or use relocationPrefix:
```gradle
shadowJar {
  mainClassName = "..."
  relocationPrefix = "shaded"
}
```

Problem: Shadow plugin **doesn't support producing multiple JARs from one config** without complex workarounds. You'd need:
1. Custom Gradle task to extract shadowJar into `lib/` directory
2. Rebuild thin app JAR
3. Track two different artifacts

**Multi-Release Support**: COMPLEX
- Extracting shadowJar into lib/*.jar files loses the merged Multi-Release structure
- Would need manual reconstruction of META-INF/versions/ directory
- Fragile: requires knowing which classes came from which dependency

**Build Changes**: MAJOR
- 30-50 lines of custom Gradle task logic
- Fragile parsing of shadowJar contents
- Error-prone: every dependency update could break it

**Implementation Complexity**: HIGH
- Custom task to unzip shadowJar
- Custom task to reconstruct individual JARs and their Multi-Release entries
- Testing complex: verify every dependency extracts correctly

**Layer Caching Effectiveness**: SAME as Approach 1 IF it works
- But requires verification that Multi-Release is preserved

**Maintenance Burden**: HIGH
- Custom code that duplicates shadowJar plugin logic
- Breakage risk if shadowing strategy changes
- Not following Gradle conventions

**Status**: NOT RECOMMENDED
- More work than Approach 1
- Doesn't align with standard Gradle practices
- Higher risk of Multi-Release corruption

---

### Approach 3: Google Jib Plugin

**How it works**: Jib bypasses Dockerfile entirely, builds Docker images directly from Gradle:
```gradle
jib {
  from.image = "eclipse-temurin:21-jre-noble"
  to.image = "docker.io/molgenis/molgenis-emx2:${project.version}"
  container.layers {
    layer("dependencies") { ... }
    layer("resources") { ... }
    layer("classes") { ... }
  }
}
```

Jib automatically splits into layers: dependencies, resources, application classes.

**Multi-Release Support**: YES
- Jib loads the shadowJar (or exploded classes)
- It can preserve JAR boundaries by NOT exploding dependencies
- But requires custom layer configuration

**Build Changes**: MEDIUM
- Add Jib plugin (~10 lines)
- Remove Docker build task from Gradle (replace with Jib)
- Remove Dockerfile (Jib generates it)

**Implementation Complexity**: MEDIUM
- Learning Jib configuration
- Requires custom layer definition if keeping individual JARs
- Can't use existing Dockerfile conventions (entrypoint.sh, custom-app mapping, python)

**Layer Caching Effectiveness**: EXCELLENT IF configured right
- Jib is designed for this exact use case
- Automatic base image updates
- Reproducible builds

**Maintenance Burden**: MEDIUM
- Jib is mature (Google maintains it)
- But shifts knowledge: Jib config instead of Dockerfile
- Harder to debug (less transparent than Dockerfile)
- Dockerfile still needed for Python, custom-app, entrypoint

**Limitations**:
- Jib wants to manage the full Docker build—we want to keep our Dockerfile
- Our current Dockerfile has custom Python install logic, custom-app volume, entrypoint.sh
- Jib doesn't easily integrate with existing Docker patterns (entrypoint.sh, VOLUME)
- Would require abandoning current Docker conventions

**Risks**:
- If Jib's layer splitting doesn't preserve Multi-Release, we're back to square one
- Jib's `-cp` classpath construction might not work with Multi-Release JARs

**Status**: NOT RECOMMENDED for this project
- Overkill for what we need (Jib = Docker image build automation)
- Loses flexibility of custom Dockerfile
- Doesn't fit with existing entrypoint.sh, python, custom-app patterns

---

### Approach 4: Spring Boot Layered JAR (without Spring Boot)

**How it works**: Spring Boot uses a special JAR format with layered archive:
```
jar tf app.jar | grep "BOOT-INF/layers"
  BOOT-INF/layers.idx          <- declares which files go in which layer
  BOOT-INF/layers/base/        <- dependencies
  BOOT-INF/layers/spring/      <- Spring framework
  BOOT-INF/layers/snapshot/    <- snapshot dependencies
  BOOT-INF/layers/application/ <- app classes
```

At build time:
```gradle
tasks.register("layerJar") {
  // extract shadowJar
  // repackage with BOOT-INF/layers structure
  // compress into new JAR
}
```

Docker unpacks per-layer:
```dockerfile
RUN mkdir -p /app/layers && \
    unzip -q /app/app.jar -d /app/layers && \
    mv /app/layers/BOOT-INF/layers/base/* /app/lib/
```

**Multi-Release Support**: NO
- Extracting the JAR explodes the Multi-Release structure
- Individual dependency JARs lose their META-INF/versions/ directory
- Truffle Multi-Release resolution breaks

**Build Changes**: LARGE
- 50-80 lines of custom Gradle task
- Must parse shadowJar, reconstruct layer boundaries
- Must preserve META-INF/versions/ when extracting

**Implementation Complexity**: HIGH
- Requires custom unzip + layer reorganization logic
- Must preserve ALL metadata for each dependency
- Fragile: dependency updates need revalidation

**Layer Caching Effectiveness**: GOOD
- Can achieve 2-3 layers (base deps, snapshot deps, app)
- But requires Docker-side extraction (Docker RUN), not just COPY

**Maintenance Burden**: HIGH
- Custom code to replicate Spring Boot's layering
- Not standard Gradle; unusual pattern
- Testing overhead: verify every layer extracts correctly

**Status**: NOT RECOMMENDED
- More work than Approach 1
- Breaks Multi-Release JAR support
- Adds Docker-side complexity (extraction in RUN layer)

---

### Approach 5: Custom Gradle Task (Dependency Unpacking)

**How it works**: Write a custom Gradle task to:
1. Resolve all runtime dependencies
2. Copy them to `build/docker/libs/`
3. Create a thin app JAR with just project classes + manifest
4. Build Docker with two COPYs

```gradle
tasks.register("prepareDependencies", Copy) {
  from configurations.runtimeClasspath
  into "build/docker/libs"
}

tasks.register("thinJar", Jar) {
  baseName = "molgenis-emx2"
  manifest { attributes "Main-Class": "..." }
  from(sourceSets.main.output.classesDirs) { exclude "**" }
  archivePath = file("build/docker/molgenis-emx2-thin.jar")
}
```

```dockerfile
COPY --link build/docker/libs/ /app/lib/
COPY --link build/docker/molgenis-emx2-thin.jar /app/app.jar
ENTRYPOINT ["java", "-cp", "/app/lib/*:/app/app.jar", "org.molgenis..."]
```

**Multi-Release Support**: YES
- Each dependency JAR copied as-is, retains Multi-Release metadata
- JVM loads individual JARs, not exploded directories
- Truffle Multi-Release resolution works

**Build Changes**: MEDIUM
- Remove shadowJar plugin
- Add prepareDependencies task (~10 lines)
- Add thinJar task (~15 lines)
- Update root build.gradle to use these instead of shadowJar

**Implementation Complexity**: MEDIUM
- Straightforward Gradle task logic
- No dependency on external plugins beyond core Gradle
- Standard patterns used by `application` plugin

**Layer Caching Effectiveness**: EXCELLENT
- deps in `/app/lib/` (changes rarely)
- app in `/app/app.jar` (changes on every commit)
- Same as Approach 1

**Maintenance Burden**: MEDIUM
- Custom Gradle code, but clear and simple
- Not using standard Gradle plugins (why reinvent?)
- More code to maintain than Approach 1

**Limitations**:
- Duplicates logic already in `application` plugin
- Requires explicit CLASSPATH management (not automatic like shadowJar)
- More test burden: verify custom tasks work across Gradle versions

**Status**: VIABLE but NOT RECOMMENDED
- Works and supports Multi-Release
- But Approach 1 is simpler (uses standard plugin)
- Only choose this if you need custom dependency filtering

---

## Comparison Matrix

| Aspect | Approach 1: application | Approach 2: Shadow Split | Approach 3: Jib | Approach 4: Layered JAR | Approach 5: Custom Task |
|--------|------------------------|-------------------------|-----------------|------------------------|------------------------|
| **Multi-Release Works** | YES ✓ | RISKY | YES (if configured) | NO ✗ | YES ✓ |
| **Build Complexity** | Very Low | High | Medium | High | Medium |
| **Gradle Changes** | 5-10 lines | 30-50 lines | 10 lines | 50-80 lines | 25 lines |
| **Docker Changes** | 3-4 lines | 3-4 lines | Complete rebuild | 5-10 lines | 3-4 lines |
| **Layer Cache Effectiveness** | 96% improvement | 96% improvement | 95% improvement | 95% improvement | 96% improvement |
| **Maintenance Burden** | Very Low | High | Medium | High | Medium |
| **Standard Gradle Pattern** | YES (plugin) | NO (custom) | YES (plugin) | NO (custom) | NO (custom) |
| **CI Pipeline Impact** | Minimal | Medium | High (replaces Docker) | Medium | Minimal |
| **Risk of Breaking Changes** | Low | High | Medium | High | Medium |
| **Tested By Community** | VERY HIGH | Low | High | Medium | Low |
| **Dockerfile Compatibility** | YES (improved) | YES | NO (replaces it) | YES | YES |
| **Python/Custom Setup** | YES | YES | Requires rework | YES | YES |

---

## Recommendation: Approach 1 (Gradle `application` Plugin)

### Why It Wins

1. **Multi-Release JAR Support**: Individual dependency JARs retain their META-INF/versions/ structure. Truffle's Java 21-optimized classes load correctly.

2. **Minimal Build Changes**: Only 5-10 lines of Gradle code. No custom logic, no hidden corners.

3. **Docker-Friendly**: Drops into existing Dockerfile with minimal changes. Preserves entrypoint.sh, custom-app, Python setup.

4. **Layer Caching**: 96% reduction in typical deploy size (10MB vs 268MB on code-only changes).

5. **Standard Practice**: Uses Gradle's built-in `application` plugin—widely used, well-documented, supported by IntelliJ, Gradle team.

6. **CI Integration**: Minimal pipeline changes. Replace JAR_FILE path from `build/libs/` to `build/install/app/lib/` + handle classpath.

7. **Maintenance**: No custom Gradle code to debug. Truffle Multi-Release just works.

8. **Testability**: Straightforward to verify: unzip any JAR from `lib/`, confirm META-INF/versions/ present.

### Implementation Outline

**1. Update root `build.gradle`** (remove shadowJar, keep application):
```gradle
plugins {
    id 'application'
}

application {
    mainClass = "org.molgenis.emx2.RunMolgenisEmx2"
}

// Remove shadowJar block entirely
```

**2. Update CI Gradle task** (in root build.gradle):
```gradle
tasks.register('dockerBuild', Exec) {
    dependsOn installDist
    // ... pass build/install/app/lib/ and classpath to Docker
}
```

**3. Update Dockerfile**:
```dockerfile
FROM eclipse-temurin:21-jre-noble

RUN apt-get update && \
    apt-get install -y --no-install-recommends python3 python3-pip python3-venv && \
    rm -rf /var/lib/apt/lists/* && \
    useradd -m molgenis

ARG JAR_FILE
COPY --link build/install/app/lib/ /app/lib/
COPY --link build/install/app/bin/molgenis-emx2 /app/bin/
COPY --link custom-app /custom-app

USER molgenis
EXPOSE 8080
ENTRYPOINT ["java"]
CMD ["-cp", "/app/lib/*:/app/app.jar", \
     "-XX:+UseG1GC", \
     "-XX:MaxRAMPercentage=75.0", \
     "org.molgenis.emx2.RunMolgenisEmx2"]
```

**4. Verify Multi-Release**:
```bash
./gradlew installDist
unzip -l build/install/app/lib/truffle-api-*.jar | grep META-INF/versions/21
# Should output versioned classes
```

### Expected Results

- **JAR distribution**: `build/install/app/lib/` contains 50+ individual JARs (~250MB)
- **App JAR**: `build/install/app/app.jar` is thin (~5-10MB, just your classes)
- **Docker build**: 2 COPY layers, independent cache invalidation
- **Typical deploy**: 10MB push (code-only change) vs 268MB today
- **Multi-Release**: Truffle Java 21 classes load correctly

---

## Alternatives Summary

**Approach 2 (Shadow Split)**: Custom extraction of shadowJar to split deps/app. Defeats purpose of using shadowJar in first place. Not recommended.

**Approach 3 (Jib)**: Google's image builder is powerful but overkill. Requires replacing Dockerfile entirely, losing customization for Python/entrypoint.sh. Better for projects without custom setup.

**Approach 4 (Layered JAR)**: Spring Boot's pattern requires reconstructing Multi-Release metadata manually. High complexity, high fragility. Only viable if willing to explode and reconstruct JAR correctly.

**Approach 5 (Custom Task)**: Viable, works, but reinvents `application` plugin. More code to maintain, no advantage over Approach 1.

---

## Next Steps (If Approved)

1. Patch root `build.gradle`: enable `application` plugin, remove shadowJar
2. Update Dockerfile: 2-layer COPY strategy, classpath in CMD
3. Test locally:
   - `./gradlew installDist` → verify lib/ and app.jar exist
   - Unzip truffle JAR → confirm META-INF/versions/ present
   - Docker build → verify python, entrypoint, app execution
4. CI integration: update dockerBuild task, verify TAG_NAME logic
5. Measure: compare push size before/after on a code-only commit

---

## Risk Assessment

**Technical Risk**: LOW
- `application` plugin is standard, stable
- Multi-Release JAR semantics are well-understood
- Individual JARs in classpath is standard practice

**Build Risk**: MEDIUM
- Need to update Gradle task for dockerBuild (JAR_FILE path changes)
- Need to test installDist output structure across Gradle versions
- Classpath management more explicit (good for clarity, bad if misconfigured)

**Deployment Risk**: LOW
- Image size reduction is a pure win
- No functionality changes (same code, same dependencies)
- Easier to debug: individual JARs vs opaque shadowJar

---

## Multi-Release JAR Verification Checklist

- [x] Truffle-api JAR exists in ~/.gradle caches
- [x] Confirmed: truffle-api-25.0.1.jar has Multi-Release: true in manifest
- [x] Confirmed: versions/9/ and versions/21/ class files present
- [x] No other dependencies use Multi-Release (only Truffle)
- [ ] (After implementation) Verify build/install/app/lib/truffle-api-*.jar retains Multi-Release
- [ ] (After implementation) Docker: unzip -l app/lib/truffle-api-*.jar | grep versions/21
- [ ] (Runtime) JVM loads versioned Truffle classes (verify with -verbose:class if needed)

