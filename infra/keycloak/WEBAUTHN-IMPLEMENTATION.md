# WebAuthn (Web Authentication) Implementation in Keycloak

## Overview

WebAuthn is a web standard for public key cryptography that enables secure, passwordless authentication on the web. By implementing WebAuthn in Keycloak, users can authenticate using biometric data (fingerprint, face recognition), security keys, or platform authenticators instead of traditional passwords.

## Benefits of WebAuthn

### Security Benefits

1. **Phishing Resistance**: WebAuthn is immune to phishing attacks because:
   - Private keys never leave the authenticator
   - Relying party ID (RP ID) must match exactly
   - Origin binding prevents fake login pages

2. **No Passwords to Steal**: Eliminates password-based attacks:
   - No password database to breach
   - No password reuse vulnerabilities
   - No password hashing/encryption concerns

3. **Strong Cryptography**: Uses proven cryptographic primitives:
   - ECDSA P-256 or Ed25519 for signatures
   - AES-GCM for encryption
   - SHA-256 for hashing

4. **Two-Factor Authentication Built-in**: Authenticator possession + user presence:
   - Device must be physically present (USB/NFC/Bluetooth)
   - User must authenticate locally (biometric/PIN)

### User Experience Benefits

1. **Passwordless Login**: No need to remember complex passwords
2. **Faster Authentication**: Sign in with a single tap/scan
3. **Cross-Device Authentication**: Use phone to authenticate on laptop
4. **Biometric Convenience**: Touch ID, Face ID, Windows Hello

### Compliance Benefits

1. **FIDO2 Compliance**: Meets modern authentication standards
2. **NIST Guidelines**: Aligns with NIST 800-63-3 authentication standards
3. **GDPR Friendly**: No personal data transmitted to server
4. **Audit Trail**: Full audit of authentication events

## Architecture

### WebAuthn Flow

```
┌─────────────────────────────────────────┐
│  1. User initiates login                 │
│  (enters username or clicks "Sign in")  │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  2. Server (Keycloak) challenges user    │
│  - Generates challenge                   │
│  - Sends challenge + RP ID               │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  3. Browser calls WebAuthn API           │
│  navigator.credentials.create()          │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  4. Authenticator creates key pair       │
│  - Prompts user (biometric/PIN)          │
│  - Generates public/private key pair     │
│  - Signs challenge                       │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  5. Browser sends response to server     │
│  - Public key                            │
│  - Signed challenge                      │
│  - Attestation                          │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  6. Server verifies signature            │
│  - Checks signature                      │
│  - Stores public key                     │
│  - Issues tokens (JWT)                   │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  7. User authenticated!                  │
│  - Full session established              │
│  - Access granted                        │
└─────────────────────────────────────────┘
```

### WebAuthn Components

```
┌─────────────────────────────────────────┐
│  Relying Party (RP)                      │
│  - Keycloak server                       │
│  - Your application                      │
│  - RP ID: bss.local                      │
│  - RP Name: BSS Platform                 │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  Client                                   │
│  - Web browser                           │
│  - WebAuthn API                          │
│  - Manages communication                 │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│  Authenticator                           │
│  - Hardware security key                 │
│  - Platform authenticator (biometric)    │
│  - Mobile authenticator                  │
│  - Roaming authenticator                 │
└─────────────────────────────────────────┘
```

## Implementation Steps

### 1. Enable WebAuthn in Keycloak

#### Option A: Import Realm Configuration

```bash
# Update the realm configuration to include WebAuthn flows
# Edit: infra/keycloak/realm-bss.json

# Add the following to the authenticationFlows section:
```

```json
{
  "authenticationFlows": [
    {
      "id": "webauthn-browser-flow",
      "alias": "WebAuthn Browser Flow",
      "description": "Browser flow with WebAuthn passwordless authentication",
      "providerId": "basic-flow",
      "topLevel": true,
      "builtIn": false,
      "authenticationExecutions": [
        {
          "authenticator": "auth-cookie",
          "authenticatorFlow": false,
          "requirement": "ALTERNATIVE",
          "priority": 10
        },
        {
          "authenticator": "webauthn-authenticator",
          "authenticatorFlow": false,
          "requirement": "ALTERNATIVE",
          "priority": 20
        }
      ]
    }
  ]
}
```

#### Option B: Admin UI (Keycloak Console)

1. **Navigate to Authentication Flows**
   - Go to: Keycloak Admin Console → Authentication → Flows

2. **Create New Flow**
   - Click "Create flow"
   - Name: "WebAuthn Browser Flow"
   - Description: "WebAuthn passwordless authentication"

3. **Add Execution Steps**
   - Add "Cookie" (alternative, priority 10)
   - Add "WebAuthn Passwordless Authenticator" (alternative, priority 20)

4. **Set as Browser Flow**
   - Go to Authentication → Flows
   - Set "WebAuthn Browser Flow" as the "Browser Flow"

5. **Configure WebAuthn Settings**
   - Go to Authentication → Policies → WebAuthn Passwordless Policy
   - Set:
     - Relying Party Entity Name: "BSS Platform"
     - Relying Party ID: "bss.local"
     - Signature Algorithms: "ES256"
     - Attestation Conveyance Preference: "indirect"
     - Attestation: "Optional"
     - Authenticator Attachment: "platform"
     - Resident Key: "preferred"
     - User Verification: "required"
     - Create Timeout: 60000 (1 minute)
     - Existing Authenticator Policy: "required"

### 2. Frontend Integration

#### Install WebAuthn Library

```bash
# Using npm
npm install @simplewebauthn/browser

# Using pnpm
pnpm add @simplewebauthn/browser
```

#### WebAuthn Service (TypeScript)

```typescript
// services/webauthn.ts
import { startAuthentication, startRegistration } from '@simplewebauthn/browser';

const RP_ID = 'bss.local';
const RP_NAME = 'BSS Platform';

interface RegistrationResponse {
  id: string;
  rawId: string;
  response: {
    attestationObject: string;
    clientDataJSON: string;
  };
  type: 'public-key';
  clientExtensionResults: any;
}

interface AuthenticationResponse {
  id: string;
  rawId: string;
  response: {
    authenticatorData: string;
    clientDataJSON: string;
    signature: string;
    userHandle: string;
  };
  type: 'public-key';
  clientExtensionResults: any;
}

export class WebAuthnService {
  // Register a new authenticator
  static async register(email: string): Promise<void> {
    // 1. Get registration options from server
    const response = await fetch('/api/webauthn/register/options', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email }),
    });

    if (!response.ok) {
      throw new Error('Failed to get registration options');
    }

    const options = await response.json();

    // 2. Start registration
    const attestation = await startRegistration(options);

    // 3. Verify registration on server
    const verificationResponse = await fetch('/api/webauthn/register/verify', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email,
        attestation,
      }),
    });

    if (!verificationResponse.ok) {
      const error = await verificationResponse.json();
      throw new Error(error.message || 'Registration failed');
    }

    const result = await verificationResponse.json();

    if (result.verified && result.registered) {
      console.log('WebAuthn registered successfully!');
    } else {
      throw new Error('Registration verification failed');
    }
  }

  // Authenticate with authenticator
  static async authenticate(email: string): Promise<void> {
    // 1. Get authentication options from server
    const response = await fetch('/api/webauthn/authenticate/options', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email }),
    });

    if (!response.ok) {
      throw new Error('Failed to get authentication options');
    }

    const options = await response.json();

    // 2. Start authentication
    const assertion = await startAuthentication(options);

    // 3. Verify authentication on server
    const verificationResponse = await fetch('/api/webauthn/authenticate/verify', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email,
        assertion,
      }),
    });

    if (!verificationResponse.ok) {
      const error = await verificationResponse.json();
      throw new Error(error.message || 'Authentication failed');
    }

    const result = await verificationResponse.json();

    if (result.verified) {
      // 4. Store authentication token
      localStorage.setItem('auth_token', result.token);
      console.log('WebAuthn authenticated successfully!');
    } else {
      throw new Error('Authentication verification failed');
    }
  }

  // Check if WebAuthn is supported
  static isSupported(): boolean {
    return (
      window.location.protocol === 'https:' ||
      window.location.hostname === 'localhost' ||
      window.location.hostname === '127.0.0.1'
    ) && !!window.PublicKeyCredential;
  }
}
```

#### Vue 3 Component

```vue
<!-- components/WebAuthnLogin.vue -->
<template>
  <div class="webauthn-login">
    <h2>Sign in with WebAuthn</h2>

    <form @submit.prevent="handleSignIn">
      <div class="form-group">
        <label for="email">Email:</label>
        <input
          id="email"
          v-model="email"
          type="email"
          required
          placeholder="user@bss.local"
        />
      </div>

      <button
        type="submit"
        :disabled="!email || loading"
        class="btn-primary"
      >
        {{ loading ? 'Signing in...' : 'Sign in with Security Key' }}
      </button>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="success" class="success">{{ success }}</p>
    </form>

    <div class="divider">or</div>

    <button
      @click="registerAuthenticator"
      :disabled="!email || loading"
      class="btn-secondary"
    >
      Register New Security Key
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { WebAuthnService } from '~/services/webauthn';

const email = ref('');
const loading = ref(false);
const error = ref('');
const success = ref('');

const handleSignIn = async () => {
  error.value = '';
  success.value = '';
  loading.value = true;

  try {
    await WebAuthnService.authenticate(email.value);
    success.value = 'Successfully signed in! Redirecting...';

    // Redirect to dashboard
    setTimeout(() => {
      window.location.href = '/dashboard';
    }, 1000);
  } catch (err: any) {
    error.value = err.message || 'Authentication failed';
  } finally {
    loading.value = false;
  }
};

const registerAuthenticator = async () => {
  error.value = '';
  success.value = '';
  loading.value = true;

  try {
    await WebAuthnService.register(email.value);
    success.value = 'Security key registered successfully!';
  } catch (err: any) {
    error.value = err.message || 'Registration failed';
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  if (!WebAuthnService.isSupported()) {
    error.value = 'WebAuthn is not supported in this browser. Please use a modern browser with WebAuthn support.';
  }
});
</script>

<style scoped>
.webauthn-login {
  max-width: 400px;
  margin: 2rem auto;
  padding: 2rem;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: bold;
}

.form-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 1rem;
}

.btn-primary,
.btn-secondary {
  width: 100%;
  padding: 0.75rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: bold;
  cursor: pointer;
  margin-bottom: 1rem;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.error {
  color: #dc3545;
  background: #f8d7da;
  padding: 0.75rem;
  border-radius: 4px;
  margin-top: 1rem;
}

.success {
  color: #155724;
  background: #d4edda;
  padding: 0.75rem;
  border-radius: 4px;
  margin-top: 1rem;
}

.divider {
  text-align: center;
  margin: 1rem 0;
  color: #666;
  position: relative;
}

.divider::before,
.divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 40%;
  height: 1px;
  background: #ddd;
}

.divider::before {
  left: 0;
}

.divider::after {
  right: 0;
}
</style>
```

### 3. Backend API Endpoints

#### Spring Boot WebAuthn Controller

```java
@RestController
@RequestMapping("/api/webauthn")
public class WebAuthnController {

    @Autowired
    private WebAuthnService webAuthnService;

    @PostMapping("/register/options")
    public ResponseEntity<RegistrationResponse> getRegistrationOptions(
            @RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            RegistrationResponse options = webAuthnService.generateRegistrationOptions(email);
            return ResponseEntity.ok(options);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(null);
        }
    }

    @PostMapping("/register/verify")
    public ResponseEntity<Map<String, Object>> verifyRegistration(
            @RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            Map<String, Object> attestation = (Map<String, Object>) request.get("attestation");

            VerificationResult result = webAuthnService.verifyRegistration(email, attestation);

            Map<String, Object> response = new HashMap<>();
            response.put("verified", result.isVerified());
            response.put("registered", result.isRegistered());

            if (result.isVerified() && result.isRegistered()) {
                response.put("token", generateJwtToken(email));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/authenticate/options")
    public ResponseEntity<AuthenticationResponse> getAuthenticationOptions(
            @RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            AuthenticationResponse options = webAuthnService.generateAuthenticationOptions(email);
            return ResponseEntity.ok(options);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(null);
        }
    }

    @PostMapping("/authenticate/verify")
    public ResponseEntity<Map<String, Object>> verifyAuthentication(
            @RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            Map<String, Object> assertion = (Map<String, Object>) request.get("assertion");

            VerificationResult result = webAuthnService.verifyAuthentication(email, assertion);

            Map<String, Object> response = new HashMap<>();
            response.put("verified", result.isVerified());

            if (result.isVerified()) {
                response.put("token", generateJwtToken(email));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    private String generateJwtToken(String email) {
        // Generate JWT token for authenticated user
        // Implementation depends on your JWT library
        return jwtTokenUtil.generateToken(email);
    }
}
```

### 4. User Registration Flow

```typescript
// Example user registration flow
async function registerUser() {
  const email = prompt('Enter your email:');
  if (!email) return;

  try {
    // 1. Register with Keycloak
    await fetch('/api/keycloak/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password: generateTempPassword() }),
    });

    // 2. Register WebAuthn authenticator
    await WebAuthnService.register(email);

    alert('User registered successfully! Please sign in.');
  } catch (error) {
    console.error('Registration failed:', error);
    alert('Registration failed. Please try again.');
  }
}
```

## WebAuthn Policy Configuration

### Keycloak WebAuthn Policy Settings

| Setting | Value | Description |
|---------|-------|-------------|
| **Relying Party Entity Name** | "BSS Platform" | Display name for the relying party |
| **Relying Party ID** | "bss.local" | Domain name (must match origin) |
| **Signature Algorithms** | ES256, EdDSA | Algorithms for key signatures |
| **Attestation Conveyance Preference** | indirect | How attestation is conveyed |
| **Attestation** | optional | Whether attestation is required |
| **Authenticator Attachment** | platform | Type of authenticators allowed |
| **Resident Key** | preferred | Whether to store credentials on device |
| **User Verification** | required | Whether user verification is required |
| **Create Timeout** | 60000 | Timeout for authenticator creation (ms) |
| **Existing Authenticator Policy** | required | How to handle existing authenticators |

## Supported Authenticators

### 1. Platform Authenticators (Built-in)

- **Touch ID** (macOS Safari)
- **Face ID** (iOS Safari)
- **Windows Hello** (Windows Edge)
- **Android Fingerprint** (Chrome on Android)

**Characteristics:**
- Built into the device
- Use biometric or PIN
- No additional hardware required
- "platform" authenticator attachment

### 2. Roaming Authenticators (External)

- **YubiKey** (USB-C, USB-A, NFC)
- **Feitian ePass** (USB, NFC)
- **Google Titan** (Bluetooth, USB)
- **SoloKeys** (Open source)

**Characteristics:**
- External device
- Can be used across multiple devices
- Phyiscal security key
- "cross-platform" authenticator attachment

## Browser Support

### WebAuthn Level 2 Support

| Browser | Version | Platform Authenticator | Roaming Authenticator |
|---------|---------|-----------------------|----------------------|
| Chrome | 90+ | ✅ | ✅ |
| Firefox | 89+ | ✅ | ✅ |
| Safari | 14+ | ✅ | ✅ |
| Edge | 90+ | ✅ | ✅ |
| Opera | 76+ | ✅ | ✅ |

### WebAuthn Browser Check

```typescript
function checkWebAuthnSupport(): {
  isSupported: boolean;
  platformAuthenticator: boolean;
  webAuthnAPI: boolean;
} {
  return {
    isSupported: !!window.PublicKeyCredential,
    platformAuthenticator: !!(window as any).PublicKeyCredential
      ?.isUserVerifyingPlatformAuthenticatorAvailable,
    webAuthnAPI: typeof navigator.credentials !== 'undefined' &&
      typeof navigator.credentials.create === 'function',
  };
}
```

## Security Considerations

### 1. HTTPS Requirement

WebAuthn **requires HTTPS** in production:

```
✅ https://bss.local           (valid)
✅ http://localhost            (exception for localhost)
✅ http://127.0.0.1            (exception for localhost)
❌ http://bss.local            (invalid)
❌ http://192.168.1.1          (invalid)
```

### 2. RP ID Validation

The Relying Party ID (RP ID) must match:
- Domain of the website
- Subdomain of the RP ID is allowed (e.g., `app.bss.local` with RP ID `bss.local`)
- Exact match for bare domain

### 3. Attestation Verification

- **None**: No attestation (most private)
- **Indirect**: Attestation with privacy protection
- **Direct**: Full attestation (can identify authenticator model)

**Recommendation**: Use `indirect` for production.

### 4. Credential Lifecycle

```typescript
// Check if credential needs to be updated
async function checkCredentialStatus(credentialId: string) {
  const response = await fetch(`/api/webauthn/credentials/${credentialId}/status`);
  return response.json();
}

// Revoke a credential
async function revokeCredential(credentialId: string) {
  await fetch(`/api/webauthn/credentials/${credentialId}`, {
    method: 'DELETE',
  });
}
```

## Testing WebAuthn

### 1. Test with Security Key

```bash
# Test YubiKey or other FIDO2 security key
# 1. Insert security key
# 2. Go to login page
# 3. Enter email
# 4. Click "Sign in with Security Key"
# 5. Touch the security key
```

### 2. Test with Browser Biometrics

```bash
# Test Touch ID (macOS)
# 1. Go to login page on Safari
# 2. Enter email
# 3. Click "Sign in with Security Key"
# 4. Authenticate with Touch ID
```

### 3. Automated Testing

```typescript
// Using WebAuthn testing library
import { virtualAuthenticator } from '@simplewebauthn/testing';

test('WebAuthn registration', async () => {
  const authenticator = await virtualAuthenticator({
    transport: 'usb',
    hasResidentKey: true,
    hasUserVerification: true,
  });

  // Test registration flow
  const registration = await startRegistration({
    ...options,
    authenticatorAttachment: 'cross-platform',
  });

  expect(registration).toBeDefined();
});
```

## Troubleshooting

### Issue 1: "WebAuthn is not supported"

**Cause:**
- Browser doesn't support WebAuthn
- Not using HTTPS
- Insecure context

**Solution:**
```typescript
// Check WebAuthn support
if (!window.PublicKeyCredential) {
  console.error('WebAuthn not supported in this browser');
  // Show fallback login
}
```

### Issue 2: "Invalid origin"

**Cause:**
- RP ID doesn't match domain
- Using HTTP instead of HTTPS
- Mismatched origins

**Solution:**
```typescript
// Ensure RP ID is correct
const rpId = window.location.hostname; // bss.local

// Or configure explicitly
const options = {
  rpId: 'bss.local', // Must match domain
};
```

### Issue 3: "Authenticator not registered"

**Cause:**
- User hasn't registered authenticator yet
- Credential was deleted
- Wrong email address

**Solution:**
```typescript
// Guide user to register first
if (error.name === 'NotAllowedError') {
  alert('Please register your security key first');
  // Redirect to registration page
}
```

### Issue 4: "User verification failed"

**Cause:**
- User didn't complete biometric/PIN
- Authenticator not configured for user verification
- Timeout occurred

**Solution:**
```typescript
try {
  await startAuthentication(options);
} catch (err) {
  if (err.name === 'SecurityError') {
    alert('Please try again and complete the biometric/PIN prompt');
  }
}
```

## Best Practices

### 1. User Experience

**Do:**
- Provide clear instructions
- Show which authenticator to use
- Offer fallback authentication
- Educate users about WebAuthn

**Don't:**
- Force users to use WebAuthn immediately
- Hide the fact that it's passwordless
- Remove password fallback entirely

### 2. Security

**Do:**
- Use HTTPS everywhere
- Set user verification to "required"
- Store credentials securely
- Audit credential usage

**Don't:**
- Allow self-signed certificates
- Use weak cryptographic algorithms
- Store private keys on server
- Disable user verification

### 3. Implementation

**Do:**
- Generate cryptographically secure challenges
- Verify signatures server-side
- Handle all WebAuthn errors
- Support multiple authenticators per user

**Don't:**
- Reuse challenges
- Trust client-side validation
- Ignore attestation
- Store raw authenticator data

## Migration Checklist

- [ ] Update Keycloak realm with WebAuthn flows
- [ ] Configure WebAuthn policy in Keycloak
- [ ] Implement backend API endpoints
- [ ] Add WebAuthn library to frontend
- [ ] Create WebAuthn login component
- [ ] Test with various authenticators
- [ ] Update documentation
- [ ] Train support team
- [ ] Monitor authentication success rates
- [ ] Create fallback authentication paths
- [ ] Add error tracking
- [ ] Set up alerting for failures

## References

- [WebAuthn Specification](https://www.w3.org/TR/webauthn-2/)
- [FIDO2 Overview](https://fidoalliance.org/fido2/)
- [Keycloak WebAuthn Documentation](https://www.keycloak.org/docs/latest/server_admin/#webauthn)
- [SimpleWebAuthn Library](https://github.com/MasterKale/SimpleWebAuthn)
- [WebAuthn Demo](https://webauthn.guide/)

## Support

For WebAuthn issues:
1. Check browser console for errors
2. Verify HTTPS is enabled
3. Check RP ID configuration
4. Contact: security@company.com
