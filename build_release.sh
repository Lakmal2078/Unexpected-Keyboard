#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ROOT_DIR}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  printf 'Error: %s not found. Copy .env.example to .env and configure signing values.\n' "${ENV_FILE}" >&2
  exit 1
fi

# .env is a trusted, shell-compatible local file. Do not source files from untrusted locations.
set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

: "${RELEASE_KEYSTORE:?Missing RELEASE_KEYSTORE in .env}"
: "${RELEASE_KEYSTORE_PASSWORD:?Missing RELEASE_KEYSTORE_PASSWORD in .env}"
: "${RELEASE_KEY_ALIAS:?Missing RELEASE_KEY_ALIAS in .env}"
: "${RELEASE_KEY_PASSWORD:?Missing RELEASE_KEY_PASSWORD in .env}"

if [[ "${RELEASE_KEYSTORE}" = /* ]]; then
  KEYSTORE_PATH="${RELEASE_KEYSTORE}"
else
  KEYSTORE_PATH="${ROOT_DIR}/${RELEASE_KEYSTORE}"
fi

if [[ ! -f "${KEYSTORE_PATH}" || ! -r "${KEYSTORE_PATH}" ]]; then
  printf 'Error: release keystore is missing or unreadable: %s\n' "${KEYSTORE_PATH}" >&2
  exit 1
fi

export RELEASE_KEYSTORE="${KEYSTORE_PATH}"
cd "${ROOT_DIR}"
exec ./gradlew clean assembleRelease
