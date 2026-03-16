#!/usr/bin/env bash
set -euo pipefail

# Generate a CI workflow for each service by substituting service name
# placeholders in the order-ci.yaml template.
#
# For each service a new branch ci-<service> is created from BASE_BRANCH,
# the generated workflow is committed, and the script returns to BASE_BRANCH.
#
# Usage: bash generate-ci.sh

SOURCE=".github/workflows/order-ci.yaml"
SERVICES=("media")

# ---------------------------------------------------------------------------

if [ ! -f "$SOURCE" ]; then
  echo "ERROR: template file not found: $SOURCE"
  exit 1
fi

BASE_BRANCH=$(git branch --show-current)

echo "Base branch : $BASE_BRANCH"
echo "Template    : $SOURCE"
echo "Services    : ${SERVICES[*]}"
echo ""

for SERVICE in "${SERVICES[@]}"; do
  echo "--- $SERVICE ---"

  # Return to the base branch before branching
  git checkout "$BASE_BRANCH"

  # Create the branch; reset it if it already exists
  if git show-ref --verify --quiet "refs/heads/ci-$SERVICE"; then
    echo "Branch ci-$SERVICE already exists, resetting to $BASE_BRANCH"
    git checkout -B "ci-$SERVICE"
  else
    git checkout -b "ci-$SERVICE"
  fi

  # Attempt to bring in any existing work from the corresponding feature branch
  git pull origin "feature/${SERVICE}_ci_test_case" || true

  # Generate the workflow by replacing all occurrences of the service name
  OUTPUT=".github/workflows/$SERVICE-ci.yaml"
  sed -e "s/order/$SERVICE/g" \
      -e "s/Order/${SERVICE^}/g" \
      "$SOURCE" > "$OUTPUT"

  echo "Created: $OUTPUT"

  git add "$OUTPUT"
  git commit -m "ci: add GitHub Actions pipeline for $SERVICE service"

  echo ""
done

git checkout "$BASE_BRANCH"
echo "Done. Created branches: ${SERVICES[*]/#/ci-}"
