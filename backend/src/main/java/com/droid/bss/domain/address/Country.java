package com.droid.bss.domain.address;

/**
 * Country codes enum (ISO 3166-1 alpha-2)
 */
public enum Country {
    PL("Poland"),
    DE("Germany"),
    FR("France"),
    ES("Spain"),
    IT("Italy"),
    UK("United Kingdom"),
    NL("Netherlands"),
    SE("Sweden"),
    NO("Norway"),
    DK("Denmark"),
    FI("Finland"),
    CZ("Czech Republic"),
    SK("Slovakia"),
    AT("Austria"),
    HU("Hungary"),
    RO("Romania"),
    BG("Bulgaria"),
    HR("Croatia"),
    SI("Slovenia"),
    EE("Estonia"),
    LV("Latvia"),
    LT("Lithuania"),
    IE("Ireland"),
    PT("Portugal"),
    GR("Greece"),
    CY("Cyprus"),
    MT("Malta"),
    LU("Luxembourg"),
    BE("Belgium");

    private final String name;

    Country(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
