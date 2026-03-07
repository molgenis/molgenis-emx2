# Efficient Docker Image

## Goal
Optimize the main Dockerfile for molgenis-emx2 to reduce image size, improve layer caching, speed up builds/deploys, and follow best practices.

## Current State
- Base: `ubuntu:24.04` with `apt update && apt -y upgrade`
- Installs: python3, python3-pip, python3-venv, openjdk-21-jre-headless via apt
- Single shadow JAR (~150-200MB) copied as one layer
- No multi-stage build
- No JAR layer splitting (every code change = full JAR re-push)
- `.dockerignore` too permissive

## Problems
1. `apt update && upgrade` — non-reproducible, slow, adds bloat and transient CVEs
2. Ubuntu base is large for a JVM app
3. Shadow JAR as single blob defeats Docker layer caching
4. `.dockerignore` sends unnecessary files to Docker daemon

## Investigation Results

### Python: REQUIRED at runtime
The `ScriptTask` system (`backend/molgenis-emx2-tasks/.../ScriptTask.java`) allows users to
execute Python scripts via the web UI. At runtime it:
- Creates a venv per script (`python3 -m venv`) → needs `python3-venv`
- Installs user-provided dependencies (`pip3 install -r requirements.txt`) → needs `python3-pip`
- Runs scripts (`python3 -u script.py`) → needs `python3`

All three packages (`python3`, `python3-pip`, `python3-venv`) are essential. Cannot be removed.

### Base image decision: NO custom base image
Maintaining a custom base image adds operational burden (security updates, versioning, registry).
Instead, use **`eclipse-temurin:21-jre-noble`** (Ubuntu 24.04 LTS based, maintained by Adoptium)
and install Python on top. This gives us:
- Professionally maintained JRE with timely security patches
- Ubuntu package ecosystem for Python (same as current)
- No `apt upgrade` needed — Temurin images are kept current
- No need to install `openjdk-21-jre-headless` ourselves

Alternative considered: Alpine-based. Rejected because Python packages users install via pip
may need C compilation (numpy, pandas) which requires build tools on Alpine (musl vs glibc issues).
Ubuntu/glibc avoids this pain for end users.

## Implementation (completed)

### Review findings applied
4 parallel reviews (correctness, efficiency, base image, Python install) identified:
- **Bug**: Builder stage needs JDK (has `jar` tool), not JRE → fixed: `eclipse-temurin:21-jdk-noble`
- **Bug**: `entrypoint.sh` may lack execute bit → fixed: added `chmod +x`
- **Improvement**: `COPY --link` for parallel BuildKit caching → applied
- **Improvement**: Merge `useradd` into Python RUN layer → applied
- **Improvement**: Split `public_html/` as separate layer (frontend vs backend) → applied
- **Improvement**: python-build-standalone instead of apt (eliminates `apt-get update`) → applied
- **Improvement**: More aggressive `.dockerignore` → applied
- **Confirmed**: `-cp "/app"` is correct (shadow JAR = flat classes, no nested JARs)
- **Confirmed**: `eclipse-temurin:21-jre-noble` is the right base (glibc for numpy/pandas, noble > jammy)
- **Confirmed**: Alpine rejected (musl breaks pip wheels for numpy/pandas)

### Final layer structure (least → most volatile)
1. `eclipse-temurin:21-jre-noble` base (~250MB, includes JRE)
2. Python via python-build-standalone (~50MB, pinned version)
3. Third-party deps from JAR (~700MB extracted, rarely changes)
4. Frontend assets `public_html/` (changes on frontend commits)
5. App classes `org/molgenis/` (~10MB, changes every backend commit)
6. `custom-app` + `entrypoint.sh` (rarely changes)

### Typical deploy pull: ~10MB (was ~268MB)

### Remaining TODO
- [ ] Test image build locally
- [ ] Verify ScriptTask Python execution works with python-build-standalone
- [ ] Verify CI pipeline compatibility (gradle dockerPush task)
- [ ] Consider multi-arch support (arm64) for python-build-standalone URL

## Status: IMPLEMENTED — awaiting local testing
