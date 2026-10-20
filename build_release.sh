#!/usr/bin/env bash
# Load environment variables from .env (ignore comments)
set -o allexport
source <(grep -v '^#' .env | xargs -d '\n')
set +o allexport

# Resolve keystore path (relative to project root)
if [[ "$RELEASE_KEYSTORE" != /* ]]; then
  RELEASE_KEYSTORE_PATH="$(pwd)/$RELEASE_KEYSTORE"
else
  RELEASE_KEYSTORE_PATH="$RELEASE_KEYSTORE"
fi

export RELEASE_KEYSTORE="$RELEASE_KEYSTORE_PATH"
# Ensure required vars are set
if [[ -z "$RELEASE_KEYSTORE_PASSWORD" || -z "$RELEASE_KEY_ALIAS" || -z "$RELEASE_KEY_PASSWORD" ]]; then
  echo "Missing signing credentials. Check .env file." >&2
  exit 1
fi

# Run Gradle clean and assembleRelease
./gradlew clean assembleRelease
