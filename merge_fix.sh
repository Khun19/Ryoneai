#!/bin/bash

# Script to merge fix/regenerate-gradle-wrapper branch into main and push changes
# Usage: bash merge_fix.sh

set -e  # Exit on error

echo "=========================================="
echo "Gradle Wrapper Fix - Merge Script"
echo "=========================================="
echo ""

# Step 1: Switch to main branch
echo "Step 1: Switching to main branch..."
git checkout main
echo "✓ Switched to main branch"
echo ""

# Step 2: Pull latest changes from remote main
echo "Step 2: Pulling latest changes from origin/main..."
git pull origin main
echo "✓ Pulled latest changes"
echo ""

# Step 3: Merge the fix branch
echo "Step 3: Merging fix/regenerate-gradle-wrapper into main..."
git merge fix/regenerate-gradle-wrapper
echo "✓ Merged fix/regenerate-gradle-wrapper"
echo ""

# Step 4: Push changes to origin
echo "Step 4: Pushing merged changes to origin/main..."
git push origin main
echo "✓ Pushed changes to origin/main"
echo ""

echo "=========================================="
echo "✅ Success! Merge completed and pushed."
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Check GitHub Actions: https://github.com/Khun19/Ryoneai/actions"
echo "2. Verify the build passes with your new gradle-wrapper.jar"
echo "3. (Optional) Clean up the fix branch:"
echo "   git branch -d fix/regenerate-gradle-wrapper"
echo "   git push origin --delete fix/regenerate-gradle-wrapper"
echo ""
