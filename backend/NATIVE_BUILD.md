# BSS Backend - GraalVM Native Image

This document describes how to build and run the BSS Backend as a GraalVM native image for improved performance and resource efficiency.

## Benefits of Native Image

- ⚡ **100x faster startup time** (typically < 100ms vs 3-5 seconds for JVM)
- 📉 **5x less memory consumption** (typically 20-30MB vs 100-200MB)
- 🔒 **Reduced attack surface** (no JIT compiler, no runtime code generation)
- 📦 **Smaller container images** (~50MB vs 300-500MB)
- 🌱 **Lower resource requirements** - perfect for serverless, containers, and edge computing

## Prerequisites

### 1. Install GraalVM

Download and install GraalVM Community Edition 21 or later:

```bash
# Download GraalVM (example for Linux)
wget https://github.com/graalvm/graalvm-ce-builds/releases/download/jvm-21.0.2%2B10/graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz
tar -xzf graalvm-community-jdk-21.0.2_linux-x64_bin.tar.gz

# Set JAVA_HOME
export JAVA_HOME=/path/to/graalvm-community-jdk-21.0.2
export PATH=$JAVA_HOME/bin:$PATH
```

### 2. Install native-image

```bash
# Using gu (GraalVM Updater)
gu install native-image
```

Verify installation:
```bash
native-image --version
```

## Building the Native Image

### Quick Start

Run the provided build script:

```bash
./build-native.sh
```

This will:
- Compile the application
- Run AOT (Ahead-of-Time) compilation
- Generate the native executable
- Display the results

### Manual Build

If you prefer manual build:

```bash
# Clean previous builds
./mvnw clean

# Build with native profile (skip tests for faster build)
./mvnw -Pnative -DskipTests package

# Or build with tests (slower, but more thorough)
./mvnw -Pnative package
```

The native executable will be created at: `target/bss-backend-native`

### Build Times

- **First build**: 5-10 minutes (downloads dependencies, runs AOT compilation)
- **Incremental builds**: 1-3 minutes (if source code hasn't changed significantly)

## Running the Native Image

### Basic Run

```bash
./target/bss-backend-native
```

### Production Profile

```bash
./target/bss-backend-native --spring.profiles.active=prod
```

### With Custom Configuration

```bash
# Use native-specific profile
./target/bss-backend-native --spring.profiles.active=native

# Or combine profiles
./target/bss-backend-native --spring.profiles.active=native,prod
```

### With Environment Variables

```bash
# PostgreSQL configuration
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB=bss
export POSTGRES_USER=bss_app
export POSTGRES_PASSWORD=password

# Redis configuration
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Run the application
./target/bss-backend-native
```

## Docker Build

Build a Docker image with the native executable:

```bash
# Build the image
docker build -f Dockerfile.native -t bss-backend-native:latest .

# Run the container
docker run -p 8080:8080 \
  -e POSTGRES_HOST=localhost \
  -e POSTGRES_PORT=5432 \
  -e POSTGRES_DB=bss \
  -e POSTGRES_USER=bss_app \
  -e POSTGRES_PASSWORD=password \
  bss-backend-native:latest
```

## Performance Comparison

| Metric | JVM (HotSpot) | Native Image | Improvement |
|--------|---------------|--------------|-------------|
| Startup Time | 3-5 seconds | < 100ms | 100x faster |
| Memory Usage | 100-200MB | 20-30MB | 5-7x less |
| Container Size | 300-500MB | ~50MB | 6-10x smaller |
| First Response | 2-3 seconds | < 50ms | 100x faster |

## Configuration

### Native-Specific Configuration

The application includes a native profile (`application-native.yml`) with optimized settings:

- Reduced connection pool sizes
- Disabled unnecessary features (GraphQL Playground, Subscriptions)
- Optimized logging
- Disabled cache warming
- Minimal actuator endpoints

### AOT Runtime Hints

The `BssRuntimeHints` class configures what should be included in the native image:

- Domain entities
- DTOs
- GraphQL resolvers
- JPA/Hibernate entities
- Jackson serialization
- Validation constraints
- Spring components
- Resources (schema files, migrations, etc.)

### Custom Build Arguments

To add custom native-image arguments, edit `pom.xml`:

```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <configuration>
        <buildArgs>
            <buildArg>--enable-http</buildArg>
            <buildArg>--enable-https</buildArg>
            <buildArg>-H:+ReportExceptionStackTraces</buildArg>
            <!-- Add your custom arguments here -->
            <buildArg>--your-custom-arg</buildArg>
        </buildArgs>
    </configuration>
</plugin>
```

## Testing

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Expected Response

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### API Endpoints

Once running, you can test the REST API:

```bash
# GET /api/v1/customers
curl http://localhost:8080/api/v1/customers

# GET /graphql (GraphQL endpoint)
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ customer(id: \"...\") { id firstName lastName email } }"}'
```

## Troubleshooting

### Build Errors

**Error**: `java.lang.RuntimeException: There was an error setting up the native-image tool`
- **Solution**: Ensure GraalVM is installed and `native-image` is in your PATH

**Error**: Out of memory during build
- **Solution**: Increase Java heap size: `./mvnw -Pnative -DskipTests -DjvmArgs="-Xmx8g" package`

**Error**: Missing reflection config
- **Solution**: Add classes to `BssRuntimeHints` class

### Runtime Errors

**Error**: `ClassNotFoundException` at runtime
- **Solution**: Add the class to `BssRuntimeHints.registerHints()`

**Error**: Resource not found
- **Solution**: Add resource patterns to `resource-config.json` or `BssRuntimeHints`

**Error**: Database connection issues
- **Solution**: Check PostgreSQL connection settings and ensure database is accessible

### Optimization Tips

1. **Use native profile**: Always use `--spring.profiles.active=native`
2. **Disable unnecessary features**: Check `application-native.yml` for feature flags
3. **Monitor memory**: Use `jcmd <pid> VM.native_memory summary` to monitor memory usage
4. **Profile startup**: Use `-H:+PrintApplicationStartupTime` to see startup breakdown

## Limitations

Some Spring Boot features have limitations in native mode:

- **Spring Boot DevTools**: Not supported (disabled in native profile)
- **Classpath scanning**: Limited; all classes must be explicitly registered
- **Reflection**: Heavily restricted; use `BssRuntimeHints` for required reflection
- **Dynamic proxies**: Limited; prefer static proxies
- **Runtime code generation**: Not available (no JIT)

## Production Deployment

For production deployment:

1. ✅ Build the native image using `build-native.sh`
2. ✅ Test thoroughly in a staging environment
3. ✅ Configure production environment variables
4. ✅ Set up monitoring (Prometheus, Grafana, etc.)
5. ✅ Use health checks for orchestration (Kubernetes, Docker Swarm, etc.)
6. ✅ Enable SSL/TLS termination at the load balancer or reverse proxy
7. ✅ Set up log aggregation (ELK, Splunk, etc.)

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build Native Image

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          distribution: 'graalvm'
          java-version: '21'
      - name: Install native-image
        run: gu install native-image
      - name: Build native image
        run: ./mvnw -Pnative -DskipTests package
      - name: Test native image
        run: ./target/bss-backend-native --spring.profiles.active=native &
        sleep 10
        curl http://localhost:8080/actuator/health
```

## Additional Resources

- [GraalVM Native Image Documentation](https://www.graalvm.org/reference-manual/native-image/)
- [Spring Boot Native](https://docs.spring.io/spring-boot/docs/current/reference/html/native-image.html)
- [Native Image Performance Guide](https://www.graalvm.org/latest/reference-manual/native-image/optimization-and-reporting/)
- [Spring AOT Engine](https://docs.spring.io/spring-framework/reference/core/aot.html)

## Support

If you encounter issues:

1. Check the troubleshooting section above
2. Review the build logs carefully
3. Consult the [Spring Boot Native documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/native-image.html)
4. Check the [GraalVM community forum](https://github.com/graalvm/graalvm/discussions)
5. Open an issue in the project repository
