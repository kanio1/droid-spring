package com.droid.bss.infrastructure.auth.oidc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents user information from OIDC UserInfo endpoint.
 *
 * @since 1.0
 */
public class UserInfo {

    private final String subject;
    private final String name;
    private final String givenName;
    private final String familyName;
    private final String middleName;
    private final String nickname;
    private final String preferredUsername;
    private final String profile;
    private final String picture;
    private final String website;
    private final String email;
    private final boolean emailVerified;
    private final String gender;
    private final LocalDate birthdate;
    private final String zoneinfo;
    private final String locale;
    private final String phoneNumber;
    private final boolean phoneNumberVerified;
    private final String address;
    private final LocalDate updatedAt;
    private final List<String> roles;
    private final List<String> permissions;
    private final Map<String, Object> customClaims;

    public UserInfo(String subject, String name, String givenName, String familyName,
                    String middleName, String nickname, String preferredUsername,
                    String profile, String picture, String website, String email,
                    boolean emailVerified, String gender, LocalDate birthdate,
                    String zoneinfo, String locale, String phoneNumber,
                    boolean phoneNumberVerified, String address, LocalDate updatedAt,
                    List<String> roles, List<String> permissions,
                    Map<String, Object> customClaims) {
        this.subject = subject;
        this.name = name;
        this.givenName = givenName;
        this.familyName = familyName;
        this.middleName = middleName;
        this.nickname = nickname;
        this.preferredUsername = preferredUsername;
        this.profile = profile;
        this.picture = picture;
        this.website = website;
        this.email = email;
        this.emailVerified = emailVerified;
        this.gender = gender;
        this.birthdate = birthdate;
        this.zoneinfo = zoneinfo;
        this.locale = locale;
        this.phoneNumber = phoneNumber;
        this.phoneNumberVerified = phoneNumberVerified;
        this.address = address;
        this.updatedAt = updatedAt;
        this.roles = roles != null ? roles : List.of();
        this.permissions = permissions != null ? permissions : List.of();
        this.customClaims = customClaims != null ? customClaims : Map.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters

    public String getSubject() {
        return subject;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getGivenName() {
        return Optional.ofNullable(givenName);
    }

    public Optional<String> getFamilyName() {
        return Optional.ofNullable(familyName);
    }

    public Optional<String> getMiddleName() {
        return Optional.ofNullable(middleName);
    }

    public Optional<String> getNickname() {
        return Optional.ofNullable(nickname);
    }

    public Optional<String> getPreferredUsername() {
        return Optional.ofNullable(preferredUsername);
    }

    public Optional<String> getProfile() {
        return Optional.ofNullable(profile);
    }

    public Optional<String> getPicture() {
        return Optional.ofNullable(picture);
    }

    public Optional<String> getWebsite() {
        return Optional.ofNullable(website);
    }

    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Optional<String> getGender() {
        return Optional.ofNullable(gender);
    }

    public Optional<LocalDate> getBirthdate() {
        return Optional.ofNullable(birthdate);
    }

    public Optional<String> getZoneinfo() {
        return Optional.ofNullable(zoneinfo);
    }

    public Optional<String> getLocale() {
        return Optional.ofNullable(locale);
    }

    public Optional<String> getPhoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    public boolean isPhoneNumberVerified() {
        return phoneNumberVerified;
    }

    public Optional<String> getAddress() {
        return Optional.ofNullable(address);
    }

    public Optional<LocalDate> getUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public Map<String, Object> getCustomClaims() {
        return customClaims;
    }

    /**
     * Gets a custom claim value.
     *
     * @param name the claim name
     * @return the claim value
     */
    public Optional<Object> getCustomClaim(String name) {
        return Optional.ofNullable(customClaims.get(name));
    }

    /**
     * Checks if user has a specific role.
     *
     * @param role the role to check
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Checks if user has a specific permission.
     *
     * @param permission the permission to check
     * @return true if user has the permission
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    /**
     * Gets a custom claim value as String.
     *
     * @param name the claim name
     * @return Optional containing the String value
     */
    public Optional<String> getCustomClaimAsString(String name) {
        Object value = customClaims.get(name);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return Objects.equals(subject, userInfo.subject) &&
               Objects.equals(email, userInfo.email) &&
               Objects.equals(preferredUsername, userInfo.preferredUsername);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, email, preferredUsername);
    }

    @Override
    public String toString() {
        return "UserInfo{" +
               "subject='" + subject + '\'' +
               ", name='" + name + '\'' +
               ", preferredUsername='" + preferredUsername + '\'' +
               ", email='" + email + '\'' +
               ", emailVerified=" + emailVerified +
               ", roles=" + roles +
               ", permissions=" + permissions +
               '}';
    }

    /**
     * Builder for UserInfo.
     */
    public static class Builder {
        private String subject;
        private String name;
        private String givenName;
        private String familyName;
        private String middleName;
        private String nickname;
        private String preferredUsername;
        private String profile;
        private String picture;
        private String website;
        private String email;
        private boolean emailVerified;
        private String gender;
        private LocalDate birthdate;
        private String zoneinfo;
        private String locale;
        private String phoneNumber;
        private boolean phoneNumberVerified;
        private String address;
        private LocalDate updatedAt;
        private List<String> roles;
        private List<String> permissions;
        private Map<String, Object> customClaims;

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder givenName(String givenName) {
            this.givenName = givenName;
            return this;
        }

        public Builder familyName(String familyName) {
            this.familyName = familyName;
            return this;
        }

        public Builder preferredUsername(String preferredUsername) {
            this.preferredUsername = preferredUsername;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder emailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public Builder roles(List<String> roles) {
            this.roles = roles;
            return this;
        }

        public Builder permissions(List<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        public Builder customClaims(Map<String, Object> customClaims) {
            this.customClaims = customClaims;
            return this;
        }

        public UserInfo build() {
            return new UserInfo(subject, name, givenName, familyName, middleName,
                              nickname, preferredUsername, profile, picture, website,
                              email, emailVerified, gender, birthdate, zoneinfo,
                              locale, phoneNumber, phoneNumberVerified, address,
                              updatedAt, roles, permissions, customClaims);
        }
    }
}
