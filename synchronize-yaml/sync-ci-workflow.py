#!/usr/bin/env python3
"""
CI Workflow Synchronization Script
====================================
Synchronizes CI workflow files across service branches using order-ci.yaml as template.

Usage:
    python sync-ci-workflow.py <service-name> [--push]

Examples:
    python sync-ci-workflow.py location          # Generate location-ci.yaml only
    python sync-ci-workflow.py location --push   # Generate and push to feature/location_ci_test_case
    python sync-ci-workflow.py --all             # Generate for all services
    python sync-ci-workflow.py --all --push      # Generate and push all services
"""

import os
import sys
import subprocess
import re
from pathlib import Path

# Fix UTF-8 encoding for Windows terminal
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

# Base configuration
# Paths are relative to project root (parent directory of synchronize-yaml)
TEMPLATE_FILE = "../.github/workflows/order-ci.yaml"
TEMPLATE_SERVICE = "order"
WORKFLOWS_DIR = "../.github/workflows"

# List of services to sync (add more as needed)
SERVICES = [
    "cart",
    "customer",
    "inventory",
    "location",
    "media",
    "product",
    "rating",
    "search",
    "tax",
]


def replace_service_name(content: str, from_service: str, to_service: str) -> str:
    """
    Replace service name in workflow content with proper case handling.

    Handles:
    - Lowercase: order -> location
    - Capitalized: Order -> Location
    - Uppercase: ORDER -> LOCATION
    - Title case in titles: "Order Coverage Report" -> "Location Coverage Report"
    """
    replacements = [
        # Title and display names (careful ordering to avoid double replacement)
        (f"{from_service} service ci", f"{to_service} service ci"),
        (f"{from_service.capitalize()} Coverage Report", f"{to_service.capitalize()} Coverage Report"),
        (f"{from_service.capitalize()}-Service-Unit-Test-Results", f"{to_service.capitalize()}-Service-Unit-Test-Results"),
        (f"yas-{from_service}", f"yas-{to_service}"),
        (f"{from_service}-jacoco-report", f"{to_service}-jacoco-report"),
        (f"{from_service}-snyk-report", f"{to_service}-snyk-report"),
        (f"{from_service}-checkstyle-result.xml", f"{to_service}-checkstyle-result.xml"),

        # File paths and module references
        (f'"{from_service}/**"', f'"{to_service}/**"'),
        (f'".github/workflows/{from_service}-ci.yaml"', f'".github/workflows/{to_service}-ci.yaml"'),
        (f"**/{from_service}-checkstyle-result.xml", f"**/{to_service}-checkstyle-result.xml"),
        (f"{from_service}/**/surefire-reports/TEST*.xml", f"{to_service}/**/surefire-reports/TEST*.xml"),
        (f"{from_service}/target/site/jacoco/jacoco.xml", f"{to_service}/target/site/jacoco/jacoco.xml"),
        (f"{from_service}/target/surefire-reports", f"{to_service}/target/surefire-reports"),
        (f"{from_service}/target/site/jacoco", f"{to_service}/target/site/jacoco"),

        # Maven commands
        (f"-pl {from_service}", f"-pl {to_service}"),
        (f"--file={from_service}/pom.xml", f"--file={to_service}/pom.xml"),
        (f"p='{from_service}/target/site/jacoco/jacoco.xml'", f"p='{to_service}/target/site/jacoco/jacoco.xml'"),
    ]

    result = content
    for old, new in replacements:
        result = result.replace(old, new)

    return result


def generate_workflow(service: str) -> bool:
    """Generate workflow file for a service from template."""
    print(f"\n📝 Generating workflow for service: {service}")

    # Read template
    template_path = Path(TEMPLATE_FILE)
    if not template_path.exists():
        print(f"❌ ERROR: Template file not found: {TEMPLATE_FILE}")
        return False

    try:
        with open(template_path, 'r', encoding='utf-8') as f:
            template_content = f.read()
    except Exception as e:
        print(f"❌ ERROR: Failed to read template: {e}")
        return False

    # Replace service name
    workflow_content = replace_service_name(template_content, TEMPLATE_SERVICE, service)

    # Write output file
    output_file = Path(WORKFLOWS_DIR) / f"{service}-ci.yaml"
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(workflow_content)
        print(f"✅ Generated: {output_file}")
        return True
    except Exception as e:
        print(f"❌ ERROR: Failed to write workflow file: {e}")
        return False


def check_git_status() -> bool:
    """Check if git working directory is clean."""
    result = subprocess.run(['git', 'status', '--porcelain'],
                          capture_output=True, text=True)
    return len(result.stdout.strip()) == 0


def push_to_branch(service: str, workflow_file: str) -> bool:
    """
    Push generated workflow file to the service's feature branch.

    Process:
    1. Stash current changes (if any)
    2. Checkout target branch
    3. Copy workflow file from main
    4. Commit and push
    5. Return to original branch
    """
    branch_name = f"feature/{service}_ci_test_case"
    original_branch = subprocess.run(['git', 'branch', '--show-current'],
                                    capture_output=True, text=True).stdout.strip()

    print(f"\n🚀 Pushing to branch: {branch_name}")

    try:
        # Check if branch exists remotely
        result = subprocess.run(['git', 'ls-remote', '--heads', 'origin', branch_name],
                              capture_output=True, text=True)
        if not result.stdout.strip():
            print(f"⚠️  WARNING: Remote branch '{branch_name}' does not exist!")
            print(f"   Please create the branch first or verify the branch name.")
            return False

        # Stash if there are changes
        has_changes = not check_git_status()
        if has_changes:
            print("💾 Stashing current changes...")
            subprocess.run(['git', 'stash', 'push', '-m', f'Auto-stash before sync {service}'], check=True)

        # Fetch latest
        print(f"📥 Fetching latest from origin/{branch_name}...")
        subprocess.run(['git', 'fetch', 'origin', branch_name], check=True)

        # Checkout target branch
        print(f"🔄 Checking out {branch_name}...")
        subprocess.run(['git', 'checkout', branch_name], check=True)

        # Pull latest changes
        print(f"📥 Pulling latest changes...")
        subprocess.run(['git', 'pull', 'origin', branch_name], check=True)

        # Copy workflow file (we need to regenerate to ensure it's in the context)
        if not generate_workflow(service):
            raise Exception("Failed to generate workflow in target branch")

        # Check if there are changes to commit
        result = subprocess.run(['git', 'status', '--porcelain', workflow_file],
                              capture_output=True, text=True)

        if not result.stdout.strip():
            print(f"ℹ️  No changes detected in {workflow_file}")
            print(f"   The workflow file is already up to date.")
        else:
            # Stage, commit and push
            print(f"📦 Staging changes...")
            subprocess.run(['git', 'add', workflow_file], check=True)

            commit_msg = f"sync: Update {service}-ci.yaml from template\n\nAuto-generated from order-ci.yaml template\n\nCo-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
            print(f"💾 Committing changes...")
            subprocess.run(['git', 'commit', '-m', commit_msg], check=True)

            print(f"⬆️  Pushing to origin/{branch_name}...")
            subprocess.run(['git', 'push', 'origin', branch_name], check=True)

            print(f"✅ Successfully pushed {workflow_file} to {branch_name}")

        # Return to original branch
        print(f"🔄 Returning to {original_branch}...")
        subprocess.run(['git', 'checkout', original_branch], check=True)

        # Pop stash if we stashed
        if has_changes:
            print("📤 Restoring stashed changes...")
            subprocess.run(['git', 'stash', 'pop'], check=True)

        return True

    except subprocess.CalledProcessError as e:
        print(f"❌ ERROR: Git command failed: {e}")
        print(f"   Attempting to return to {original_branch}...")
        subprocess.run(['git', 'checkout', original_branch], check=False)
        if has_changes:
            subprocess.run(['git', 'stash', 'pop'], check=False)
        return False
    except Exception as e:
        print(f"❌ ERROR: {e}")
        subprocess.run(['git', 'checkout', original_branch], check=False)
        if has_changes:
            subprocess.run(['git', 'stash', 'pop'], check=False)
        return False


def print_usage():
    """Print usage instructions."""
    print(__doc__)


def main():
    """Main entry point."""
    if len(sys.argv) < 2:
        print_usage()
        sys.exit(1)

    # Parse arguments
    service = sys.argv[1]
    should_push = '--push' in sys.argv

    # Handle --all flag
    if service == '--all':
        print(f"🔄 Processing all {len(SERVICES)} services...")
        success_count = 0
        failed_services = []

        for svc in SERVICES:
            workflow_file = f".github/workflows/{svc}-ci.yaml"

            # Generate
            if generate_workflow(svc):
                success_count += 1

                # Push if requested
                if should_push:
                    if not push_to_branch(svc, workflow_file):
                        failed_services.append(svc)
            else:
                failed_services.append(svc)

        # Summary
        print(f"\n{'='*60}")
        print(f"📊 SUMMARY")
        print(f"{'='*60}")
        print(f"✅ Successfully processed: {success_count}/{len(SERVICES)} services")
        if failed_services:
            print(f"❌ Failed services: {', '.join(failed_services)}")
        print(f"{'='*60}\n")

        sys.exit(0 if not failed_services else 1)

    # Validate service name
    if service not in SERVICES:
        print(f"❌ ERROR: Unknown service '{service}'")
        print(f"\nAvailable services: {', '.join(SERVICES)}")
        print(f"\nTo add a new service, edit the SERVICES list in this script.")
        sys.exit(1)

    # Generate workflow
    workflow_file = f".github/workflows/{service}-ci.yaml"
    if not generate_workflow(service):
        sys.exit(1)

    # Push if requested
    if should_push:
        if not push_to_branch(service, workflow_file):
            sys.exit(1)
        print(f"\n✨ Done! Workflow synced to feature/{service}_ci_test_case")
    else:
        print(f"\n✨ Done! Review the changes and use --push to push to branch.")
        print(f"   Command: python sync-ci-workflow.py {service} --push")


if __name__ == '__main__':
    main()
