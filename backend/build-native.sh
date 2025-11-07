#!/bin/bash
set -e

echo "=================================="
echo "BSS Backend - Native Build Script"
echo "=================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if GraalVM is installed
if ! command -v native-image &> /dev/null; then
    echo -e "${RED}Error: native-image not found in PATH${NC}"
    echo -e "${YELLOW}Please install GraalVM and native-image:${NC}"
    echo "  1. Download GraalVM: https://www.graalvm.org/downloads/"
    echo "  2. Set JAVA_HOME to GraalVM installation"
    echo "  3. Install native-image: gu install native-image"
    exit 1
fi

echo -e "${GREEN}✓ Found native-image${NC}"

# Check Java version
java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{print $1"."$2}')
echo -e "${GREEN}✓ Java version: ${java_version}${NC}"

# Build the native image
echo -e "\n${YELLOW}Building native image...${NC}"
echo "This may take several minutes..."
echo ""

# Run the Maven build with native profile
./mvnw -Pnative -DskipTests clean package

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}=================================="
    echo -e "Native image built successfully!"
    echo -e "==================================${NC}"
    echo ""
    echo "Executable location: target/bss-backend-native"
    echo ""

    # Show file size
    if [ -f "target/bss-backend-native" ]; then
        size=$(du -h target/bss-backend-native | cut -f1)
        echo -e "${GREEN}Image size: ${size}${NC}"
    fi

    echo ""
    echo "To run the native image:"
    echo "  ./target/bss-backend-native --spring.profiles.active=prod"
    echo ""

    echo "To test the image:"
    echo "  curl http://localhost:8080/actuator/health"
    echo ""
else
    echo -e "${RED}=================================="
    echo -e "Native build failed!"
    echo -e "==================================${NC}"
    exit 1
fi
