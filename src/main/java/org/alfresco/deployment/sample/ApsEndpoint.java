package org.alfresco.deployment.sample;

public class ApsEndpoint {
    private String revisionVersion;
    private String edition;
    private String type;
    private String majorVersion;
    private String minorVersion;

    public ApsEndpoint() {
    }

    public String getRevisionVersion() {
        return revisionVersion;
    }

    public void setRevisionVersion(String revisionVersion) {
        this.revisionVersion = revisionVersion;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(String majorVersion) {
        this.majorVersion = majorVersion;
    }

    public String getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(String minorVersion) {
        this.minorVersion = minorVersion;
    }

    @Override
    public String toString() {
        return "{\"edition\":\""+edition+"\",\"majorVersion\":\""+majorVersion+"\",\"minorVersion\":\""+minorVersion+"\"}";
    }
}
