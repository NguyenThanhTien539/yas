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
SERVICES=("customer")

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
  MERGED_EXISTING=false

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
  FEATURE_BRANCH="feature/${SERVICE}_ci_test_case"
  echo "Attempting to merge existing work from $FEATURE_BRANCH..."

  # First, fetch to ensure we have latest remote refs
  git fetch origin || echo "Fetch failed, continuing..."

  # Check if remote branch exists
  if git ls-remote --exit-code --heads origin "$FEATURE_BRANCH" >/dev/null 2>&1; then
    echo "Remote branch $FEATURE_BRANCH found. Attempting to merge..."

    # Try to merge the remote branch
    if ! git merge "origin/$FEATURE_BRANCH" --no-edit 2>/dev/null; then
      # Check if we're in a conflicted state
      if git status --porcelain | grep -q "^UU\|^AA\|^DD"; then
        echo ""
        echo "🔥 MERGE CONFLICT DETECTED!"
        echo "Files in conflict:"
        git status --porcelain | grep "^UU\|^AA\|^DD"
        echo ""
        echo "Please resolve conflicts manually:"
        echo "  1. Edit conflicted files in your editor"
        echo "  2. Run: git add <resolved-files>"
        echo "  3. Run: git commit (or git merge --abort to skip)"
        echo ""
        echo -n "Press Enter when ready to continue (or Ctrl+C to exit): "
        read -r
        echo "Continuing..."
      else
        echo "Merge failed for unknown reason. Continuing with clean branch..."
      fi
    else
      echo "Successfully merged existing work from $FEATURE_BRANCH"
      MERGED_EXISTING=true
    fi
  else
    echo "Remote branch $FEATURE_BRANCH not found. Creating new workflow..."
  fi

  # Generate the workflow by replacing all occurrences of the service name
  OUTPUT=".github/workflows/$SERVICE-ci.yaml"

  # Only generate from template if we didn't successfully merge existing work
  if [ "${MERGED_EXISTING:-false}" = true ] && [ -f "$OUTPUT" ]; then
    echo "Using existing workflow: $OUTPUT (merged from $FEATURE_BRANCH)"
  else
    echo "Generating workflow from template..."
    sed -e "s/order/$SERVICE/g" \
        -e "s/Order/${SERVICE^}/g" \
        "$SOURCE" > "$OUTPUT"
    echo "Created: $OUTPUT"
  fi

  git add "$OUTPUT"
  git commit -m "ci: add GitHub Actions pipeline for $SERVICE service"

  echo ""
done

git checkout "$BASE_BRANCH"
echo "Done. Created branches: ${SERVICES[*]/#/ci-}"
