package com.transit.delay_prediction.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Agency {
    @Id
    private String agencyId;
    private String agencyName;
    private String agencyUrl;
    private String agencyTimezone;
    private String agencyLang;
    private String agencyPhone;

    // Getters and setters
    public String getAgencyId() { return agencyId; }
    public void setAgencyId(String agencyId) { this.agencyId = agencyId; }
    public String getAgencyName() { return agencyName; }
    public void setAgencyName(String agencyName) { this.agencyName = agencyName; }
    public String getAgencyUrl() { return agencyUrl; }
    public void setAgencyUrl(String agencyUrl) { this.agencyUrl = agencyUrl; }
    public String getAgencyTimezone() { return agencyTimezone; }
    public void setAgencyTimezone(String agencyTimezone) { this.agencyTimezone = agencyTimezone; }
    public String getAgencyLang() { return agencyLang; }
    public void setAgencyLang(String agencyLang) { this.agencyLang = agencyLang; }
    public String getAgencyPhone() { return agencyPhone; }
    public void setAgencyPhone(String agencyPhone) { this.agencyPhone = agencyPhone; }
}