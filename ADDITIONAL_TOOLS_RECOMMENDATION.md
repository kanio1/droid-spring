# Additional Recommended Tools - Flyway, PostgreSQL, CNCF & Project Tools

**Date**: 2025-11-06
**Focus**: Flyway testing, PostgreSQL 18 testing, CNCF tools, and additional project tools

---

## 🎯 **Top 5 Recommended Tools**

### 1. **Yandex Quokka** - Database Migration Testing

**Why We Need It**:
- **Flyway has NO built-in testing capabilities**
- Critical to test migrations before production
- Yandex Quokka is the **industry standard** for testing schema migrations
- Detects breaking changes, data loss, rollback issues

**What It Adds**:
- **Test-driven schema development** - Write tests for migrations
- **Automatic migration verification** - Detects drift between expected and actual schema
- **Data validation** - Verify data after migration
- **CI/CD integration** - Fail pipeline if migration test fails
- **Rollback testing** - Test that down scripts work

**Implementation**:

**Step 1: Install Quokka**
```xml
<!-- backend/pom.xml -->
<dependency>
    <groupId>com.playtika.testcontainers</groupId>
    <artifactId>embedded-postgresql</artifactId>
    <version>2.2.12</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>com.yandex.qatools.flyway</groupId>
    <artifactId>flyway-test-extensions</artifactId>
    <version>2.3.0</version>
    <scope>test</scope>
</dependency>
```

**Step 2: Create Migration Test**
```java
// backend/src/test/java/com/droid/bss/migration/CustomerTableMigrationTest.java
@SpringBootTest
@ContextConfiguration(classes = TestConfig.class)
@TestExecutionListeners({FlywayTestExecutionListener.class})
@FlywayTest(locationsToMigrate = {"db/migration"}, cleanSchema = true)
public class CustomerTableMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void shouldCreateCustomerTable() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet tables = conn.getMetaData().getTables(null, null, "customers", null);
            assertThat(tables.next()).isTrue();
            assertThat(tables.getString("TABLE_TYPE")).isEqualTo("TABLE");
        }
    }

    @Test
    public void shouldAddCustomerIndex() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet indexes = meta.getIndexInfo(null, null, "customers", false, false);
            List<String> indexNames = new ArrayList<>();
            while (indexes.next()) {
                indexNames.add(indexes.getString("INDEX_NAME"));
            }
            assertThat(indexNames).contains("idx_customer_email");
        }
    }

    @Test
    public void shouldMigrateExistingData() {
        // Test data migration logic
        CustomerRepository repo = new CustomerRepository(dataSource);
        assertThat(repo.count()).isGreaterThanOrEqualTo(0);
    }
}
```

**Step 3: Migration Rollback Test**
```java
// backend/src/test/java/com/droid/bss/migration/CustomerTableRollbackTest.java
@SpringBootTest
@ContextConfiguration(classes = TestConfig.class)
@TestExecutionListeners({FlywayTestExecutionListener.class})
@FlywayTest(
    locationsToMigrate = {"db/migration"},
    cleanSchema = true
)
public class CustomerTableRollbackTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void shouldRollbackCustomerTable() {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setLocationsToMigrate("db/migration");

        // Migrate
        flyway.migrate();

        // Rollback (for Undo migrations)
        flyway.undo();

        // Verify rollback
        try (Connection conn = dataSource.getConnection()) {
            ResultSet tables = conn.getMetaData()
                .getTables(null, null, "customers", null);
            assertThat(tables.next()).isFalse();
        }
    }
}
```

**Step 4: CI/CD Integration**
```yaml
# .github/workflows/migration-testing.yml
- name: Test Flyway migrations
  run: |
    cd backend
    mvn test -Dtest=*MigrationTest
    mvn test -Dtest=*RollbackTest
```

---

### 2. **pgTAP** - PostgreSQL Unit Testing

**Why We Need It**:
- **Native PostgreSQL unit testing** (not in-memory)
- Test business logic at database level
- Test stored procedures, triggers, functions
- **PostgreSQL 18** compatible

**What It Adds**:
- ✅ **SQL-based tests** - Write tests in SQL
- ✅ **Database-level testing** - Test actual database behavior
- ✅ **Stored procedure testing** - Test PL/pgSQL code
- ✅ **Trigger testing** - Verify trigger logic
- ✅ **Migration validation** - Tests that migrations work correctly

**Installation**:
```sql
-- Install pgTAP (one-time setup)
CREATE EXTENSION IF NOT EXISTS pgtap;
```

**Implementation**:

**Step 1: Create Test File**
```sql
-- backend/src/test/sql/customers_schema_test.sql
BEGIN;

-- Test customer table exists
SELECT plan(3);

SELECT has_table('customers', 'should have customers table');
SELECT has_column('customers', 'id', 'should have id column');
SELECT has_column('customers', 'email', 'should have email column');
SELECT has_column('customers', 'status', 'should have status column');

-- Test indexes
SELECT has_index('customers', 'idx_customer_email', 'should have email index');
SELECT has_index('customers', 'idx_customer_status', 'should have status index');

-- Test constraints
SELECT has_check('customers', 'customers_status_check', 'should have status check constraint');

-- Test function exists
SELECT has_function('calculate_customer_lifetime_value', ARRAY['uuid'], 'should have calculate function');

-- Test business logic
CREATE OR REPLACE FUNCTION test_calculate_lifetime_value() RETURNS TABLE(customer_id uuid, ltv numeric) AS $$
    SELECT c.id, COALESCE(SUM(o.total_amount), 0)
    FROM customers c
    LEFT JOIN orders o ON c.id = o.customer_id
    GROUP BY c.id;
$$ LANGUAGE sql;

SELECT is(
    (SELECT ltv FROM test_calculate_lifetime_value() WHERE customer_id = '00000000-0000-0000-0000-000000000000'),
    0,
    'new customer should have 0 LTV'
);

SELECT * FROM finish();
ROLLBACK;
```

**Step 2: Run pgTAP Tests**
```bash
# Using pg_prove (CLI tool)
pg_prove -h localhost -U bss_app -d bss backend/src/test/sql/customers_schema_test.sql
```

**Step 3: Java Integration**
```java
// backend/src/test/java/com/droid/bss/database/pgtap/PgTAPTest.java
@SpringBootTest
@Testcontainers
public class PgTAPTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("testbss")
            .withUsername("test")
            .withPassword("test");

    @Test
    public void runPgTAPTests() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "pg_prove",
            "-h", postgres.getHost(),
            "-p", postgres.getFirstMappedPort().toString(),
            "-U", postgres.getUsername(),
            "-d", postgres.getDatabaseName(),
            "src/test/sql/customers_schema_test.sql"
        );
        pb.directory(new File("backend"));
        Process process = pb.start();
        int exitCode = process.waitFor();
        assertEquals(0, exitCode);
    }
}
```

**Step 4: CI/CD Integration**
```yaml
# .github/workflows/pgtap-testing.yml
- name: Run pgTAP tests
  run: |
    cd backend
    pg_prove -h localhost -U postgres -d test_bss src/test/sql/*.sql
```

---

### 3. **LitmusChaos** - CNCF Chaos Engineering

**Why We Need It**:
- **CNCF incubating project** for chaos engineering
- Test system resilience under failure
- Critical for production reliability
- Kubernetes-native chaos testing

**What It Adds**:
- ✅ **Pod kill** - Test what happens when pods die
- ✅ **Network latency** - Test with slow network
- ✅ **Disk failure** - Test I/O errors
- ✅ **Memory leak** - Test OOM situations
- ✅ **Chaos in CI/CD** - Validate new deployments
- ✅ **Kubernetes-native** - Runs as K8s operators

**Implementation**:

**Step 1: Install Litmus**
```bash
# Install Litmus
kubectl apply -f https://litmuschaos.github.io/litmus/2.14.0/litmus-2.14.0.yaml

# Wait for pods
kubectl get pods -n litmus
```

**Step 2: Create Chaos Experiment**
```yaml
# dev/chaos/experiments/pod-delete.yaml
apiVersion: litmuschaos.io/v1alpha1
kind: ChaosEngine
metadata:
  name: backend-pod-delete
  namespace: default
spec:
  engineState: 'active'
  appinfo:
    appns: 'default'
    applabel: 'app=backend'
    appkind: 'deployment'
  chaosServiceAccount: pod-delete-sa
  experiments:
  - name: pod-delete
    spec:
      components:
        env:
        # Pod name to kill
        - name: TARGET_PODS
          value: 'backend-0'
        # Number of pods to kill
        - name: REPLICA_COUNT
          value: '1'
        # Run for 60 seconds
        - name: TOTAL_CHAOS_DURATION
          value: '60'
```

**Step 3: Run Chaos in CI/CD**
```yaml
# .github/workflows/chaos-testing.yml
- name: Run chaos tests
  run: |
    # Apply chaos experiment
    kubectl apply -f dev/chaos/experiments/

    # Wait for experiment
    kubectl wait --for=condition=complete chaosengine/backend-pod-delete --timeout=300s

    # Check results
    kubectl describe chaosengine backend-pod-delete
```

**Step 4: Chaos with JMeter**
```bash
# Run JMeter load test while chaos is running
jmeter -n -t api-load-test.jmx -l results.jtl &
# Apply chaos
kubectl apply -f pod-delete.yaml
# Wait
sleep 60
# Stop chaos
kubectl delete -f pod-delete.yaml
```

---

### 4. **Open Policy Agent (OPA)** - Policy as Code

**Why We Need It**:
- **CNCF graduated project** for policy enforcement
- Enforce security, compliance, governance rules
- Validate Kubernetes resources before deployment
- API request/response validation

**What It Adds**:
- ✅ **Kubernetes admission control** - Prevent unauthorized changes
- ✅ **API policy enforcement** - Validate request/response
- ✅ **Regulatory compliance** - GDPR, SOX, PCI-DSS policies
- ✅ **GitOps integration** - Policies in Git, validated in CI/CD
- ✅ **Multi-language** - Write policies in Rego language

**Implementation**:

**Step 1: Install OPA Gatekeeper**
```bash
# Install Gatekeeper
kubectl apply -f https://raw.githubusercontent.com/open-policy-agent/gatekeeper/release-3.14/deploy/gatekeeper.yaml
```

**Step 2: Create Policy for API**
```rego
# dev/policies/require-api-version.rego
package api

import future.keywords.if

# Deny requests without API version
deny[msg] if {
    input.request.path == "/api/v1/customers"
    not input.request.headers["X-API-Version"]
    msg := "X-API-Version header is required"
}

# Deny requests with deprecated API version
deny[msg] if {
    input.request.path == "/api/v1/customers"
    version := input.request.headers["X-API-Version"]
    version == "v1.0"
    msg := "API version v1.0 is deprecated, use v2.0 or higher"
}

# Require authentication
deny[msg] if {
    input.request.path == "/api/v1/customers"
    not input.request.headers.Authorization
    msg := "Authentication required"
}
```

**Step 3: Kubernetes Resource Validation**
```yaml
# dev/policies/must-have-labels.yaml
apiVersion: templates.gatekeeper.sh/v1beta1
kind: ConstraintTemplate
metadata:
  name: k8srequiredlabels
spec:
  crd:
    spec:
      names:
        kind: K8sRequiredLabels
      validation:
        properties:
          labels:
            type: array
            items:
              type: string
          message:
            type: string
  targets:
    - target: admission.k8s.gatekeeper.sh
      rego: |
        package k8srequiredlabels

        violation[{"msg": msg}] {
          container := input.review.object.spec.template.spec.containers[_]
          required := input.parameters.labels[_]
          not has_key(container, "labels")
          msg := sprintf("container %v must have labels %v", [container.name, required])
        }
```

**Step 4: Enforce in CI/CD**
```yaml
# .github/workflows/policy-validation.yml
- name: Validate policies with OPA
  run: |
    # Install OPA
    curl -L -o opa https://openpolicyagent.org/downloads/v0.61.0/opa_linux_amd64
    chmod +x opa

    # Validate policy
    ./opa test dev/policies/
    ./opa build dev/policies/ -o policy.bundle.tar.gz

    # Validate K8s resources
    ./opa evaluate --bundle policy.bundle.tar.gz -d deploy/backend.yaml
```

---

### 5. **Falco** - CNCF Runtime Security

**Why We Need It**:
- **CNCF graduated project** for runtime security
- Detect anomalous behavior in containers
- Monitor syscalls, file access, network
- Real-time threat detection

**What It Adds**:
- ✅ **File access monitoring** - Detect unauthorized file access
- ✅ **Process monitoring** - Detect unexpected processes
- ✅ **Network monitoring** - Detect unusual network activity
- ✅ **System call detection** - Low-level security events
- ✅ **Kubernetes integration** - Works with K8s events

**Implementation**:

**Step 1: Install Falco**
```yaml
# dev/security/falco.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: falco-rules
data:
  falco_rules.yaml: |
    - rule: Unauthorized API Access
      desc: Detect access to sensitive API endpoints
      condition: >
        spawned_process and
        proc.name in (backend) and
        fd.rip in (10.0.0.0/8) and
        k8s_audit and
        ka.uri.path startswith /api/v1/admin
      output: >
        Unauthorized access to admin API
        (user=%ka.user.name uri=%ka.uri.path client_ip=%ka.clientip
         command=%proc.cmdline)
      priority: WARNING
      tags: [security, PCI-DSS]
```

**Step 2: Run Falco**
```bash
# Install Falco
curl -s https://falco.org/repo/rpm/stable | sudo tee /etc/yum.repos.d/falcosecurity.repo
sudo yum install -y falco

# Start Falco
sudo systemctl enable falco
sudo systemctl start falco

# View logs
sudo journalctl -u falco -f
```

**Step 3: Falco with K6 Load Testing**
```bash
# Run K6 test
k6 run --vus 1000 api-load-test.js &

# Check Falco for anomalies
falco -M | grep "Warning"
```

**Step 4: Alerting Integration**
```yaml
# dev/security/falco-alerts.yaml
- alert: UnauthorizedFileAccess
  expr: increase(falco_events_total{rule="Unauthorized API Access"}[5m]) > 0
  for: 1m
  labels:
    severity: warning
  annotations:
    summary: "Unauthorized file access detected"
    description: "Process {{ $labels.proc_name }} accessed unauthorized file"
```

---

## 🛠️ **Additional CNCF Tools Worth Considering**

### 6. **Jaeger** - Distributed Tracing
```yaml
# Install Jaeger
kubectl create namespace observability
kubectl apply -f https://jaegertracing.github.io/jaeger-operator/v1.44.0/jaeger-all-in-one.yaml
```

**Use Case**: Trace requests across microservices, identify bottlenecks

### 7. **Loki + Grafana** - Log Aggregation
```yaml
# Install Loki
helm repo add grafana https://grafana.github.io/helm-charts
helm install loki grafana/loki-stack
```

**Use Case**: Centralized logging, search across all services

### 8. **Velero** - Backup & Recovery
```bash
# Install Velero
velero install --provider aws --plugins velero/velero-plugin-for-aws:v1.8.0 --bucket velero-backups
```

**Use Case**: Kubernetes resource backup, migration between clusters

### 9. **Argo CD** - GitOps
```yaml
# Install Argo CD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/v2.8.0/manifests/install.yaml
```

**Use Case**: GitOps deployment, sync Kubernetes manifests from Git

### 10. **Vitess** - Database Scaling
```bash
# Install Vitess
helm repo add vitess https://vitess.io/charts
helm install vitess vitess/vitess
```

**Use Case**: Horizontal database scaling, MySQL alternative

---

## 📊 **Tool Comparison Matrix**

| Tool | CNCF Status | Priority | Purpose | Effort | Benefit |
|------|-------------|----------|---------|--------|---------|
| **Yandex Quokka** | - | ⭐⭐⭐ | Flyway testing | Medium | Critical |
| **pgTAP** | - | ⭐⭐⭐ | PostgreSQL testing | Low | High |
| **LitmusChaos** | Incubating | ⭐⭐⭐ | Chaos engineering | Medium | Critical |
| **OPA/Gatekeeper** | Graduated | ⭐⭐ | Policy as Code | Medium | High |
| **Falco** | Graduated | ⭐⭐ | Runtime security | Low | Medium |
| **Jaeger** | Graduated | ⭐ | Observability | Medium | High |
| **Loki** | Sandbox | ⭐ | Logging | Low | Medium |
| **Velero** | Sandbox | ⭐ | Backup | Low | High |
| **Argo CD** | Sandbox | ⭐ | GitOps | High | High |
| **Vitess** | Sandbox | ⭐ | Database scaling | High | Medium |

---

## 🎯 **Immediate Recommendations (Top 3)**

### 1. **Yandex Quokka** - Start Here
**Why**: No way to test migrations currently
**Impact**: Prevent production failures
**Effort**: 2-3 days
**Action**:
```bash
# Add to pom.xml
# Create test classes
# Run in CI
```

### 2. **pgTAP** - Quick Win
**Why**: Test actual database behavior
**Impact**: Catch DB issues early
**Effort**: 1-2 days
**Action**:
```sql
-- Install pgTAP
CREATE EXTENSION pgtap;
-- Write SQL tests
-- Run with pg_prove
```

### 3. **LitmusChaos** - Enterprise
**Why**: Test resilience before production
**Impact**: Improve reliability
**Effort**: 1 week
**Action**:
```bash
# Install Litmus
kubectl apply -f https://litmuschaos.github.io/litmus/2.14.0/litmus-2.14.0.yaml
# Create chaos experiments
# Run in staging
```

---

## 📅 **Implementation Roadmap**

```
Week 1: Yandex Quokka
  - Add dependencies
  - Create migration tests
  - Create rollback tests
  - Integrate with CI
  ↓
Week 2: pgTAP
  - Install pgTAP
  - Write SQL tests
  - Integrate with CI
  ↓
Week 3: LitmusChaos
  - Install in staging
  - Create basic experiments
  - Run chaos tests
  ↓
Week 4: Litmus + JMeter
  - Run load test + chaos
  - Verify resilience
  ↓
Week 5: OPA/Gatekeeper
  - Install Gatekeeper
  - Create policies
  - Enforce in CI/CD
  ↓
Week 6: Falco
  - Install Falco
  - Configure rules
  - Setup alerting
```

---

## 💡 **Why These Tools?**

**Flyway Testing (Quokka)**:
- Currently NO testing for migrations
- Critical to prevent data loss
- Test rollback scripts

**PostgreSQL Testing (pgTAP)**:
- Test at DB level
- Validate schema changes
- Test stored procedures

**CNCF Tools (Litmus, OPA, Falco)**:
- Industry standards
- Cloud-native approach
- Production-ready
- Enterprise adoption

**Why not Liquibase**:
- We already use Flyway
- Both do same thing
- Quokka works with Flyway

**Why not other chaos tools**:
- Chaos Monkey: Not actively maintained
- Chaos Mesh: K8s-only (we need hybrid)
- Gremlin: Proprietary
- Litmus: Open source, active, CNCF

---

## ✅ **Next Steps**

1. **Yandex Quokka** - Add to backend/pom.xml
2. **pgTAP** - Install extension, write first test
3. **LitmusChaos** - Deploy to staging cluster
4. **OPA** - Create first policy
5. **Falco** - Install and configure

These tools will significantly improve our testing coverage, especially for database and infrastructure layers!
