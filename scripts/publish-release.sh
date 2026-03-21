#!/usr/bin/env bash
# File: `scripts/publish-release.sh`
# Usage: ./scripts/publish-release.sh [VERSION]
# Creates git tag vVERSION, pushes it, and creates a GitHub release.
# Requires: git, curl (and either gh CLI or GITHUB_TOKEN env var)

set -euo pipefail

# Helper: get latest v* tag (most recent by semantic sort)
get_latest_tag() {
  git tag --list 'v*' --sort=-v:refname 2>/dev/null | head -n1 || true
}

ARG_VERSION="${1:-}"
REMOTE="${2:-origin}"

# Ensure we have a clean working tree (optional safety)
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Working tree is dirty. Commit or stash changes before creating a release."
  exit 1
fi

CUR_TAG=$(get_latest_tag)
if [[ -z "${CUR_TAG}" ]]; then
  CUR_VERSION="none"
else
  # strip leading v
  CUR_VERSION="${CUR_TAG#v}"
fi

# Determine default version
if [[ -n "${ARG_VERSION}" ]]; then
  DEFAULT_VERSION="${ARG_VERSION}"
else
  if [[ "${CUR_VERSION}" == "none" ]]; then
    DEFAULT_VERSION="1.0.0"
  else
    IFS='.' read -r MAJOR MINOR PATCH <<<"${CUR_VERSION}"
    # If parse fails, fallback
    if [[ -z "${PATCH:-}" ]]; then
      DEFAULT_VERSION="1.0.0"
    else
      PATCH=$((PATCH + 1))
      DEFAULT_VERSION="${MAJOR}.${MINOR}.${PATCH}"
    fi
  fi
fi

echo "Current version: ${CUR_VERSION}"
read -r -p "New version [${DEFAULT_VERSION}]: " INPUT_VERSION
if [[ -n "${INPUT_VERSION}" ]]; then
  VERSION="${INPUT_VERSION}"
else
  VERSION="${DEFAULT_VERSION}"
fi

TAG="v${VERSION}"

# Prompt to edit changelog
CHANGELOG="CHANGELOG.md"
read -r -p "Edit ${CHANGELOG} now to add notes for v${VERSION}? [Y/n] " EDIT_CHOICE
EDIT_CHOICE="${EDIT_CHOICE:-Y}"
if [[ "${EDIT_CHOICE}" =~ ^[Yy] ]]; then
  if [[ ! -f "${CHANGELOG}" ]]; then
    cat > "${CHANGELOG}" <<EOF
# Changelog

All notable changes to this project will be documented in this file.

## ${TAG} - $(date +%F)

- Describe changes here.

EOF
  else
    # Prepend a header for the new version to the top for convenience
    TMPFILE="$(mktemp)"
    {
      echo "## ${TAG} - $(date +%F)"
      echo
      echo "- Describe changes here."
      echo
      cat "${CHANGELOG}"
    } > "${TMPFILE}"
    mv "${TMPFILE}" "${CHANGELOG}"
  fi

  # Prefer nano for editing the changelog if available; otherwise fall back to EDITOR/VISUAL or vi.
  if command -v nano >/dev/null 2>&1; then
    EDITOR="nano"
  else
    : "${EDITOR:=${VISUAL:-vi}}"
  fi
  "${EDITOR}" "${CHANGELOG}"
else
  echo "Make sure to update ${CHANGELOG} with notes for ${TAG} before publishing."
fi

# Create annotated tag if it doesn't exist
if git rev-parse "${TAG}" >/dev/null 2>&1; then
  echo "Tag ${TAG} already exists locally."
else
  git tag -a "${TAG}" -m "Release ${VERSION}"
  echo "Created tag ${TAG}."
fi

# Push tag
git push "${REMOTE}" "${TAG}"
echo "Pushed tag ${TAG} to ${REMOTE}."

# Allow explicit override via env vars (useful in CI or when remote detection fails)
if [[ -n "${GITHUB_OWNER:-}" && -n "${GITHUB_REPO:-}" ]]; then
  OWNER="${GITHUB_OWNER}"
  REPO="${GITHUB_REPO}"
  echo "Using OWNER/REPO from environment: ${OWNER}/${REPO}"
else
  # Determine repo owner/name from origin url (used for GitHub API and JitPack coordinates)
  # Prefer `git config --get remote.<name>.url` which is more portable in some environments
  RAW_ORIGIN_URL=$(git config --get remote."${REMOTE}".url 2>/dev/null || true)
  # If not present in config, try `git remote get-url`
  if [[ -z "${RAW_ORIGIN_URL}" ]]; then
    RAW_ORIGIN_URL=$(git remote get-url "${REMOTE}" 2>/dev/null || true)
  fi
  # Fallback: parse `git remote -v` output for origin (fetch) if still empty
  if [[ -z "${RAW_ORIGIN_URL}" ]]; then
    RAW_ORIGIN_URL=$(git remote -v 2>/dev/null | awk '/^origin\s+/{print $2; exit}' || true)
  fi
  ORIGIN_URL="${RAW_ORIGIN_URL:-}"
  OWNER=""
  REPO=""
  if [[ -n "${ORIGIN_URL}" ]]; then
    # Normalize common prefixes and formats to a plain host/path form:
    # Examples -> github.com/owner/repo
    NORMALIZED="${ORIGIN_URL}"
    # Remove git+ssh://, ssh://, git://, https://, http:// prefixes
    NORMALIZED="${NORMALIZED#git+ssh://}"
    NORMALIZED="${NORMALIZED#ssh://}"
    NORMALIZED="${NORMALIZED#git://}"
    NORMALIZED="${NORMALIZED#https://}"
    NORMALIZED="${NORMALIZED#http://}"
    # Convert git@github.com:owner/repo.git -> git@github.com/owner/repo.git
    NORMALIZED="${NORMALIZED/:/\/}"
    # If it started with git@, drop the user prefix so we have github.com/owner/repo.git
    NORMALIZED="${NORMALIZED#git@}"
    # Trim any trailing .git
    NORMALIZED="${NORMALIZED%.git}"

    # Now try to extract owner/repo from the normalized form
    if [[ "${NORMALIZED}" =~ github\.com/([^/]+)/([^/]+)$ ]]; then
      OWNER="${BASH_REMATCH[1]}"
      REPO="${BASH_REMATCH[2]}"
    else
      # As a last resort, split on '/' and take the last two path components
      IFS='/' read -r -a PARTS <<<"${NORMALIZED}"
      LEN=${#PARTS[@]}
      if (( LEN >= 2 )); then
        OWNER="${PARTS[LEN-2]}"
        REPO="${PARTS[LEN-1]}"
      fi
    fi
  fi
fi

# If detection failed, print a helpful debug line for the user (but continue)
if [[ -z "${OWNER}" || -z "${REPO}" ]]; then
  echo "Warning: unable to auto-detect OWNER/REPO from remote '${REMOTE}'. Origin URL: '${ORIGIN_URL}'"
  echo "If this is unexpected, ensure your remote is set to GitHub (for example: git@github.com:Mallen220/TurtleTracerLib.git)"
  echo "To set the correct origin URL run:"
  echo "  git remote set-url ${REMOTE} git@github.com:Mallen220/TurtleTracerLib.git"
else
  echo "Detected GitHub repo: ${OWNER}/${REPO} (from ${ORIGIN_URL})"
fi

# Try gh CLI first
if command -v gh >/dev/null 2>&1; then
  echo "Using gh CLI to create release..."
  gh release create "${TAG}" --title "v${VERSION}" --notes-file "${CHANGELOG}" --notes "Release ${VERSION}"
  echo "Release v${VERSION} created via gh."
else
  # Fallback to GitHub API using GITHUB_TOKEN
  if [[ -z "${GITHUB_TOKEN:-}" ]]; then
    echo "gh CLI not found and GITHUB_TOKEN is not set. Install gh or set GITHUB_TOKEN."
    exit 1
  fi

  if [[ -z "${OWNER}" || -z "${REPO}" ]]; then
    echo "Unable to parse GitHub repo from origin URL: ${ORIGIN_URL}"
    exit 1
  fi

  API_URL="https://api.github.com/repos/${OWNER}/${REPO}/releases"

  # Use changelog content if available for body
  if [[ -f "${CHANGELOG}" ]]; then
    # Extract the top section for this version (until next "## " header) to include as body
    RELEASE_BODY="$(awk '/^## /{if (c++==0) {print; next} else exit} {if (c==0) print}' "${CHANGELOG}" | sed '1d' || true)"
  else
    RELEASE_BODY="Release ${VERSION}"
  fi

  # JSON encode simple body (safe for most contents)
  POST_DATA=$(cat <<EOF
{
  "tag_name": "${TAG}",
  "name": "v${VERSION}",
  "body": $(printf '%s' "${RELEASE_BODY}" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))'),
  "draft": false,
  "prerelease": false
}
EOF
)

  echo "Creating release via GitHub API for ${OWNER}/${REPO}..."
  HTTP_RESPONSE=$(curl -sS -o /dev/stderr -w "%{http_code}" \
    -X POST "${API_URL}" \
    -H "Authorization: token ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -d "${POST_DATA}" 2>/dev/null || true)

  if [[ "${HTTP_RESPONSE}" == "201" ]]; then
    echo "Release v${VERSION} created successfully."
  else
    echo "Failed to create release (HTTP ${HTTP_RESPONSE})."
    exit 1
  fi
fi

# At this point the GitHub release/tag exists. Publish to JitPack by triggering a build and show coordinates.
if [[ -n "${OWNER}" && -n "${REPO}" ]]; then
  JITPACK_BASE="https://jitpack.io"
  # JitPack expects the tag name as the version; recommend using the tag including the leading 'v' if that's the tag on GitHub.
  JITPACK_VERSION="${TAG}"
  echo
  echo "Triggering JitPack build for ${OWNER}/${REPO} tag ${JITPACK_VERSION}..."
  JITPACK_BUILD_URL="${JITPACK_BASE}/com/github/${OWNER}/${REPO}/${JITPACK_VERSION}/build.log"

  # Fetch build log (this triggers the build). Save to a temp file and show a short tail so users can see progress.
  TMP_JITLOG="$(mktemp)"
  HTTP_CODE=$(curl -sS -w "%{http_code}" -o "${TMP_JITLOG}" "${JITPACK_BUILD_URL}" || true)
  if [[ "${HTTP_CODE}" == "200" ]]; then
    echo "JitPack build triggered. Showing last 40 lines of build log (if any):"
    tail -n 40 "${TMP_JITLOG}" || true
  else
    echo "Unable to fetch JitPack build log (HTTP ${HTTP_CODE}). You can visit: ${JITPACK_BUILD_URL} to trigger or view the build."
  fi
  rm -f "${TMP_JITLOG}"

  # Poll for the published POM file (artifactId = REPO, file: <REPO>-<VERSION>.pom)
  echo
  echo "Waiting for JitPack to publish POM for ${REPO} (version: ${JITPACK_VERSION})..."
  POM_URLS=(
    "${JITPACK_BASE}/com/github/${OWNER}/${REPO}/${JITPACK_VERSION}/${REPO}-${JITPACK_VERSION}.pom"
    "${JITPACK_BASE}/com/github/${OWNER}/${REPO}/${JITPACK_VERSION#v}/${REPO}-${JITPACK_VERSION#v}.pom"
  )

  FOUND_POM=""
  MAX_ATTEMPTS=10
  SLEEP_SECONDS=3
  for ((i=1;i<=MAX_ATTEMPTS;i++)); do
    for url in "${POM_URLS[@]}"; do
      code=$(curl -sS -o /dev/null -w "%{http_code}" "${url}" || true)
      if [[ "${code}" == "200" ]]; then
        FOUND_POM="${url}"
        break 2
      fi
    done
    echo "  Attempt ${i}/${MAX_ATTEMPTS}: POM not available yet. Sleeping ${SLEEP_SECONDS}s..."
    sleep ${SLEEP_SECONDS}
  done

  if [[ -n "${FOUND_POM}" ]]; then
    echo "POM available at: ${FOUND_POM}"
    # Download to ./build or ./release if exists
    OUT_DIR="./build/release"
    mkdir -p "${OUT_DIR}"
    OUT_FILE="${OUT_DIR}/${REPO}-${JITPACK_VERSION}.pom"
    curl -sS -o "${OUT_FILE}" "${FOUND_POM}" || true
    echo "Downloaded POM to: ${OUT_FILE}"
  else
    echo "Timed out waiting for POM. If the build is still running, check: ${JITPACK_BASE}/com/github/${OWNER}/${REPO}/${JITPACK_VERSION}/"
  fi

  echo
  echo "JitPack coordinates (use these in your build):"
  echo
  echo "Add the JitPack repository to your build system if you haven't already:"
  echo "  Gradle (settings.gradle or build.gradle):"
  echo "    repositories { maven { url 'https://jitpack.io' } }"
  echo
  echo "Then add the dependency using the tag name (here we show both common forms):"
  echo "  Gradle (Groovy): implementation 'com.github.${OWNER}:${REPO}:${JITPACK_VERSION}'"
  echo "  Gradle (Kotlin): implementation(\"com.github.${OWNER}:${REPO}:${JITPACK_VERSION}\")"
  echo
  echo "  Maven:"
  echo "    <dependency>"
  echo "      <groupId>com.github.${OWNER}</groupId>"
  echo "      <artifactId>${REPO}</artifactId>"
  echo "      <version>${JITPACK_VERSION}</version>"
  echo "    </dependency>"
  echo
  echo "Alternatively, if you prefer to drop the leading 'v' in the version, use '${JITPACK_VERSION#v}' as the version in the coordinates above."
else
  echo "Owner/repo not detected; skipping JitPack publish instructions. Ensure your remote origin points to GitHub to use JitPack."
fi

exit 0
