# Security Testing Suite

This directory contains a comprehensive **Security Testing Suite** to validate the BSS application's security posture and identify vulnerabilities.

## Overview

The security testing suite includes:

1. **OWASP ZAP Active Scanning** - Automated penetration testing
2. **Security Headers Validation** - HTTP security headers verification
3. **Authentication & Authorization Testing** - Access control validation
4. **Nuclei Vulnerability Scanning** - Known vulnerability detection
5. **Input Validation Testing** - SQL injection, XSS prevention
6. **Session Management Testing** - Session security validation
7. **Password Security Testing** - Password policy enforcement

## Prerequisites

### 1. Install Playwright

```bash
npx playwright install
```

### 2. Install OWASP ZAP (Optional)

**Option A: Docker**
```bash
docker run -d -p 8080:8080 -p 8090:8090 \
  -v "$(pwd)":/zap/wrk/:rw \
  owasp/zap2docker-stable zap.sh \
  -daemon -port 8080 -api8090 \
  -config api.key=test -config api.addrs.addr.name=.* \
  -config api.addrs.addr.regex=true
```

**Option B: Standalone**
```bash
# Download from https://www.zaproxy.org/download/
# Start ZAP in daemon mode
./zap.sh -daemon -port 8080 -api8090 -config api.key=test
```

### 3. Install Nuclei (Optional)

```bash
# Linux
curl -L https://github.com/projectdiscovery/nuclei/releases/latest/download/nuclei_2.9.15_linux_amd64.zip -o nuclei.zip
unzip nuclei.zip -d /tmp && sudo mv /tmp/nuclei /usr/local/bin/

# macOS
brew install nuclei

# Verify
nuclei -version
```

## Environment Setup

```bash
# Set application URL
export BASE_URL=http://localhost:3000

# Set ZAP proxy (if using ZAP)
export ZAP_PROXY=http://localhost:8080

# Create results directory
mkdir -p results/security
```

## Test Files

### 1. OWASP ZAP Active Scan (`zap-active-scan.spec.ts`)

**Purpose:** Automated security scanning using OWASP ZAP

**What it tests:**
- SQL injection vulnerabilities
- Cross-Site Scripting (XSS)
- Directory traversal
- Insecure HTTP methods
- CSRF vulnerabilities
- And 100+ other OWASP Top 10 issues

**Configuration:**
```typescript
const BASE_URL = process.env.BASE_URL || 'http://localhost:3000'
const ZAP_PROXY = process.env.ZAP_PROXY || 'http://localhost:8080'
```

**Run:**
```bash
# Through ZAP proxy
ZAP_PROXY=http://localhost:8080 \
npx playwright test zap-active-scan.spec.ts

# Individual test
npx playwright test zap-active-scan.spec.ts --grep "ZAP Active Security Scan"
```

**Expected Output:**
```
=== ZAP ACTIVE SCAN RESULTS ===
Total issues found: 15
Risk Summary:
  Critical: 0
  High: 2
  Medium: 5
  Low: 8
  Informational: 0

Top Findings:
1. [HIGH] SQL Injection
2. [MEDIUM] X-Frame-Options Missing
```

---

### 2. Security Headers Validation (`security-headers.spec.ts`)

**Purpose:** Validate HTTP security headers configuration

**What it tests:**
- Strict-Transport-Security (HSTS)
- X-Content-Type-Options (nosniff)
- X-Frame-Options (DENY/SAMEORIGIN)
- X-XSS-Protection
- Content-Security-Policy (CSP)
- Referrer-Policy
- Permissions-Policy
- Cookie security flags (Secure, HttpOnly, SameSite)
- Server header information disclosure
- X-Powered-By disclosure

**Test Suites:**
1. **Login Page** - Headers on public pages
2. **Dashboard Page** - Headers on authenticated pages
3. **API Endpoints** - Headers on API responses
4. **Cookie Security** - Cookie attributes
5. **HTTPS Configuration** - TLS/SSL settings

**Run:**
```bash
npx playwright test security-headers.spec.ts
```

**Expected Headers:**
```
strict-transport-security: max-age=31536000
x-content-type-options: nosniff
x-frame-options: SAMEORIGIN
x-xss-protection: 1; mode=block
content-security-policy: default-src 'self'; script-src 'self'
```

**Failures:**
- Missing HSTS → Users vulnerable to man-in-the-middle attacks
- Missing X-Frame-Options → Vulnerable to clickjacking
- Weak CSP → Vulnerable to XSS attacks
- Cookies without Secure/HttpOnly → Session hijacking risk

---

### 3. Authentication & Authorization Security (`auth-security.spec.ts`)

**Purpose:** Validate authentication and access control mechanisms

**What it tests:**
- Brute force protection
- SQL injection in login forms
- Username enumeration prevention
- Session timeout
- Authentication requirements for protected routes
- Privilege escalation prevention
- Direct object reference (IDOR) prevention
- CSRF protection
- Re-authentication for sensitive operations
- Session fixation prevention
- Session invalidation on logout
- Concurrent session control
- Password policy enforcement
- Password reuse prevention
- Account deletion security

**Test Suites:**
1. **Authentication Security** - Login mechanisms
2. **Authorization Security** - Access control
3. **Session Management Security** - Session handling
4. **Password Security** - Password policies

**Run:**
```bash
npx playwright test auth-security.spec.ts
```

**Expected Behaviors:**
- Failed login attempts trigger rate limiting
- No user enumeration (generic error messages)
- Session expires after inactivity
- Cannot access other users' resources
- CSRF token present on forms
- Password changes require re-authentication
- Sessions invalidated on logout
- Weak passwords rejected

**Failed Scenarios:**
- Allows unlimited login attempts → Brute force vulnerability
- Reveals if user exists → Username enumeration
- Session doesn't expire → Session hijacking risk
- Can access other users' data → Authorization bypass
- No CSRF token → CSRF vulnerability
- Accepts weak passwords → Weak security

---

### 4. Nuclei Vulnerability Scanner (`nuclei-scan.js`)

**Purpose:** Scan for known vulnerabilities using Nuclei

**What it detects:**
- Outdated software versions
- Known CVEs
- Misconfigurations
- Information disclosure
- SSL/TLS issues
- And 5000+ other vulnerability patterns

**Configuration:**
- Templates from: https://github.com/projectdiscovery/nuclei-templates
- Severity filter: High, Critical, Medium
- Rate limit: 100 requests/second
- Timeout: 5 seconds

**Run:**
```bash
# Automatic install if nuclei not found
node tests/security/nuclei-scan.js

# With custom target
BASE_URL=https://your-app.com node tests/security/nuclei-scan.js
```

**Output Files:**
- `results/nuclei-results-YYYY-MM-DD-HH-MM-SS.json` - Full results
- `results/nuclei-results-YYYY-MM-DD-HH-MM-SS-summary.txt` - Summary

**Sample Output:**
```
=== NUCLEI VULNERABILITY SCAN ===
Target: http://localhost:3000
Output Directory: ./results

Scan Results Summary:
  Total Findings: 3
  Critical: 0
  High: 1
  Medium: 2
  Low: 0
  Info: 0

Top Findings:
1. [HIGH] Missing Security Headers
2. [MEDIUM] SSL Certificate Verification
3. [MEDIUM] Information Disclosure

✓ No critical or high severity vulnerabilities found
```

---

### 5. Input Validation Testing

**Purpose:** Validate that user input is properly sanitized

**SQL Injection Tests:**
- `' OR '1'='1` - Authentication bypass
- `'; DROP TABLE users; --` - Data deletion
- `' UNION SELECT * FROM users --` - Data extraction
- `1' OR 1=1#` - Boolean-based injection

**XSS Tests:**
- `<script>alert("XSS")</script>` - Script execution
- `"><script>alert("XSS")</script>` - Attribute injection
- `javascript:alert('XSS')` - Protocol handler
- `<img src=x onerror=alert("XSS")>` - Event handler

**Directory Traversal Tests:**
- `../../../etc/passwd` - Unix systems
- `..\\..\\..\\windows\\system32\\drivers\\etc\\hosts` - Windows systems
- `%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd` - URL encoded

**Command Injection Tests:**
- `; cat /etc/passwd` - Command chaining
- `| whoami` - Pipe injection
- `$(whoami)` - Command substitution

## Running Tests

### Run All Security Tests

```bash
# Make script executable
chmod +x tests/security/security-tests.sh

# Run full suite
./tests/security/security-tests.sh

# Skip ZAP (if not running)
SKIP_ZAP=true ./tests/security/security-tests.sh

# Skip Nuclei (if nuclei not installed)
SKIP_NUCLEI=true ./tests/security/security-tests.sh

# Custom configuration
BASE_URL=http://staging.example.com \
ZAP_PROXY=http://zap:8080 \
./tests/security/security-tests.sh
```

### Run Specific Test Category

```bash
# Security headers only
npx playwright test security-headers.spec.ts

# Authentication security only
npx playwright test auth-security.spec.ts

# ZAP scan only (requires ZAP running)
ZAP_PROXY=http://localhost:8080 \
npx playwright test zap-active-scan.spec.ts

# Nuclei scan only
node tests/security/nuclei-scan.js
```

### Run with Custom Results Directory

```bash
# Save to specific directory
export RESULTS_DIR=./security-results-$(date +%Y%m%d)
npx playwright test security/ --output="$RESULTS_DIR"
```

## Test Configuration

### Playwright Config (`playwright.config.ts`)

Add security test project:

```typescript
{
  name: 'security',
  testDir: './tests/security',
  testMatch: /.*\.spec\.ts/,
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:3000',
    ...devices['Desktop Chrome'],
    headless: true,
  },
  timeout: 300000, // 5 minutes
  retries: 1,
}
```

### Environment Variables

```bash
# Application URL (required)
BASE_URL=http://localhost:3000

# ZAP Proxy URL (optional)
ZAP_PROXY=http://localhost:8080

# Results directory
RESULTS_DIR=./results/security

# Nuclei API key (if using Nuclei Cloud)
NUCLEI_TOKEN=your-token

# Custom templates directory
NUCLEI_TEMPLATES=/path/to/templates
```

## Interpreting Results

### Security Headers

**Critical Headers (Must Have):**
- ✅ `strict-transport-security: max-age=...`
- ✅ `x-content-type-options: nosniff`
- ✅ `x-frame-options: DENY or SAMEORIGIN`
- ✅ `content-security-policy: ...`

**Important Headers (Should Have):**
- ✅ `x-xss-protection: 1; mode=block`
- ✅ `referrer-policy: strict-origin or no-referrer`
- ✅ `permissions-policy: ...`

**Cookie Security:**
- ✅ `Secure` flag (over HTTPS)
- ✅ `HttpOnly` flag
- ✅ `SameSite` attribute (Lax, Strict, or None)

### ZAP Scan Results

**Risk Levels:**
- **Critical:** Immediate security risk (SQL injection, RCE, etc.)
- **High:** Significant security risk (XSS, CSRF, etc.)
- **Medium:** Moderate security risk (weak headers, etc.)
- **Low:** Minor security issue (info disclosure, etc.)
- **Informational:** No security risk, informational only

**Pass Criteria:**
- 0 Critical vulnerabilities
- 0 High vulnerabilities (recommended)

### Nuclei Scan Results

**Severity Levels:**
- **Critical:** CVSS 9.0-10.0
- **High:** CVSS 7.0-8.9
- **Medium:** CVSS 4.0-6.9
- **Low:** CVSS 0.1-3.9
- **Info:** Informational only

**Pass Criteria:**
- 0 Critical vulnerabilities
- 0 High vulnerabilities

### Authentication & Authorization

**Must Pass:**
- ✅ Rate limiting on login
- ✅ No username enumeration
- ✅ Session timeout (≤ 1 hour)
- ✅ Protected routes require auth
- ✅ No privilege escalation
- ✅ No IDOR vulnerabilities
- ✅ CSRF protection
- ✅ Session invalidation on logout

## Continuous Integration

### GitHub Actions

```yaml
name: Security Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 2 * * *' # Daily at 2 AM

jobs:
  security-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '20'

      - name: Install dependencies
        run: npm install

      - name: Install Playwright
        run: npx playwright install

      - name: Start application
        run: |
          docker-compose up -d
          sleep 30

      - name: Run security tests
        run: |
          chmod +x tests/security/security-tests.sh
          BASE_URL=http://localhost:3000 ./tests/security/security-tests.sh
        env:
          BASE_URL: http://localhost:3000

      - name: Upload security results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: security-test-results
          path: results/security/
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any

    environment {
        BASE_URL = 'http://staging.example.com'
    }

    stages {
        stage('Security Tests') {
            steps {
                script {
                    sh '''
                        chmod +x tests/security/security-tests.sh
                        ./tests/security/security-tests.sh
                    '''
                }
            }

            post {
                always {
                    archiveArtifacts artifacts: 'results/security/**', allowEmptyArchive: true
                }
            }
        }
    }
}
```

## Troubleshooting

### ZAP Scan Issues

**Problem:** "ZAP is not running"
```bash
# Start ZAP in Docker
docker run -d -p 8080:8080 owasp/zap2docker-stable zap.sh -daemon

# Verify
curl http://localhost:8080
```

**Problem:** "Scan timeout"
```typescript
// Increase timeout in test
test('ZAP scan', async ({ page }, testInfo) => {
  testInfo.timeout(600000) // 10 minutes
  // ...
})
```

**Problem:** "Too many requests"
```javascript
// Reduce scan intensity in ZAP
// Or add delays between requests
await page.waitForTimeout(2000)
```

### Nuclei Scan Issues

**Problem:** "Nuclei not found"
```bash
# Manual install
curl -L https://github.com/projectdiscovery/nuclei/releases/latest/download/nuclei_2.9.15_linux_amd64.zip -o nuclei.zip
unzip nuclei.zip && sudo mv nuclei /usr/local/bin/
```

**Problem:** "No templates found"
```bash
# Update nuclei templates
nuclei -update-templates
```

**Problem:** "Too many findings"
```bash
# Use severity filter
nuclei -target $BASE_URL -severity high,critical
```

### Security Headers Issues

**Problem:** "Headers not being set"
- Check your reverse proxy configuration (nginx, Apache, Caddy)
- Verify application is not overriding headers
- Check for middleware that removes headers

**Problem:** "CSP too restrictive"
- Test in staging first
- Gradually add allowed sources
- Use CSP violation reports

### Authentication Test Issues

**Problem:** "Test fails on login"
- Check test user credentials
- Verify application is running
- Check for anti-automation measures

**Problem:** "Session doesn't timeout"
- Lower timeout for testing (e.g., 1 minute)
- Check session configuration
- Verify no "Remember me" is active

## Security Best Practices

### 1. Regular Scanning
- Run security tests daily
- Scan before every release
- Monitor for new vulnerabilities
- Keep tools updated

### 2. Defense in Depth
- Security headers (first line of defense)
- Input validation (application layer)
- Authentication/Authorization (access control)
- Network security (firewalls, WAF)
- Infrastructure security (OS, dependencies)

### 3. Secure Development
- Security requirements in specs
- Threat modeling
- Security code reviews
- Developer security training
- OWASP Top 10 awareness

### 4. Incident Response
- Document vulnerabilities
- Prioritize by risk
- Track remediation
- Verify fixes
- Learn from incidents

## Security Test Checklist

**Pre-Deployment:**
- [ ] All security tests pass
- [ ] No Critical/High vulnerabilities
- [ ] Security headers properly configured
- [ ] Authentication mechanisms secure
- [ ] Authorization controls verified
- [ ] Input validation in place
- [ ] Session management secure
- [ ] Dependencies up to date
- [ ] SSL/TLS properly configured
- [ ] Error handling secure

**Production Monitoring:**
- [ ] WAF configured
- [ ] Rate limiting enabled
- [ ] Log monitoring active
- [ ] Alerting configured
- [ ] Regular security audits
- [ ] Penetration testing scheduled
- [ ] Vulnerability scanning scheduled
- [ ] Security metrics tracked

## Common Vulnerabilities

### OWASP Top 10 (2021)

1. **A01: Broken Access Control**
   - Test: auth-security.spec.ts - privilege escalation
   - Test: auth-security.spec.ts - IDOR prevention

2. **A02: Cryptographic Failures**
   - Test: security-headers.spec.ts - HTTPS enforcement
   - Test: security-headers.spec.ts - HSTS

3. **A03: Injection**
   - Test: zap-active-scan.spec.ts - SQL injection
   - Test: zap-active-scan.spec.ts - XSS

4. **A04: Insecure Design**
   - Covered in: architecture review

5. **A05: Security Misconfiguration**
   - Test: security-headers.spec.ts
   - Test: nuclei-scan.js

6. **A06: Vulnerable Components**
   - Test: nuclei-scan.js - component scanning

7. **A07: Authentication Failures**
   - Test: auth-security.spec.ts - brute force
   - Test: auth-security.spec.ts - session management

8. **A08: Data Integrity Failures**
   - Test: auth-security.spec.ts - CSRF

9. **A09: Security Logging Failures**
   - Review: application logs

10. **A10: Server-Side Request Forgery**
    - Test: zap-active-scan.spec.ts - SSRF

## Additional Resources

- [OWASP ZAP Documentation](https://www.zaproxy.org/docs/)
- [Nuclei Documentation](https://docs.nuclei.sh/)
- [Playwright Security Testing](https://playwright.dev/docs/test-configuration)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Web Security Cheat Sheet](https://cheatsheetseries.owasp.org/)
- [Security Headers](https://securityheaders.com/)

## Support

For issues or questions:
1. Check troubleshooting section
2. Review test logs
3. Verify environment setup
4. Check tool documentation
5. Contact security team

---

**Last Updated:** 2025-11-06
**Test Types:** ZAP, Nuclei, Headers, Auth, Input Validation
**Tools:** OWASP ZAP, Nuclei, Playwright
**Coverage:** OWASP Top 10, Security Headers, Session Management
