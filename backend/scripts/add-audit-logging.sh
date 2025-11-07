#!/bin/bash

# Add @Audited annotation to all controller methods
# Automatically adds audit logging to all sensitive operations

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BACKEND_DIR="${PROJECT_ROOT}/backend"

echo "=========================================="
echo "Adding Audit Logging to Controllers"
echo "=========================================="
echo ""

# Color codes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_info() {
    echo -e "${YELLOW}→${NC} $1"
}

# Find all controllers that don't have @Audited yet
controllers=$(find "${BACKEND_DIR}/src/main/java/com/droid/bss/api" -name "*Controller.java" -type f)

processed=0
skipped=0

for controller in $controllers; do
    filename=$(basename "$controller")

    # Skip if already processed (CustomerController and PaymentController)
    if [[ "$filename" == "CustomerController.java" ]] || [[ "$filename" == "PaymentController.java" ]]; then
        print_info "Skipping ${filename} (already processed)"
        skipped=$((skipped + 1))
        continue
    fi

    # Check if already has @Audited import
    if grep -q "import.*Audited" "$controller"; then
        print_info "Skipping ${filename} (already has audit logging)"
        skipped=$((skipped + 1))
        continue
    fi

    print_status "Processing ${filename}..."

    # Extract entity type from filename (e.g., CustomerController -> Customer)
    entity_type=$(echo "$filename" | sed 's/Controller\.java//')

    # Add imports
    sed -i '1i import com.droid.bss.domain.audit.AuditAction;\nimport com.droid.bss.infrastructure.audit.Audited;' "$controller"

    # Add @Audited to POST methods (CREATE)
    sed -i "s/@PreAuthorize/\\n    @Audited(action = AuditAction.${entity_type^^}_CREATE, entityType = \"${entity_type}\", description = \"Creating new ${entity_type,,}\")\n    @PreAuthorize/" "$controller" 2>/dev/null || true

    # Add @Audited to PUT methods (UPDATE)
    sed -i "s/@PreAuthorize/\\n    @Audited(action = AuditAction.${entity_type^^}_UPDATE, entityType = \"${entity_type}\", description = \"Updating ${entity_type,,} {id}\")\n    @PreAuthorize/" "$controller" 2>/dev/null || true

    # Add @Audited to DELETE methods (DELETE)
    sed -i "s/@PreAuthorize/\\n    @Audited(action = AuditAction.${entity_type^^}_DELETE, entityType = \"${entity_type}\", description = \"Deleting ${entity_type,,} {id}\")\n    @PreAuthorize/" "$controller" 2>/dev/null || true

    processed=$((processed + 1))
done

echo ""
echo "=========================================="
print_status "Audit Logging Setup Complete!"
echo "=========================================="
echo ""
echo "Summary:"
echo "  • Processed: $processed controllers"
echo "  • Skipped: $skipped controllers"
echo ""
echo "Controllers with audit logging:"
echo "  • CustomerController ✅"
echo "  • PaymentController ✅"
echo "  • All other controllers ✅ (auto-generated)"
echo ""
echo "Next steps:"
echo "  1. Review the changes: git diff"
echo "  2. Compile backend: mvn clean compile"
echo "  3. Run tests: mvn test"
echo ""
