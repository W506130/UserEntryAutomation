import java.util.List;

public class UserProfile {
    private String userId;
    private AccessRoles role;
    private String emailAddress;
    private String displayName;
    private String otherPhone;
    private String mobilePhone;
    private Long clientGroupMid;
    private List<AccountProfile> accountProfiles;
    private String status;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public AccessRoles getRole() {
        return role;
    }

    public void setRole(AccessRoles role) {
        this.role = role;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getOtherPhone() {
        return otherPhone;
    }

    public void setOtherPhone(String otherPhone) {
        this.otherPhone = otherPhone;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public Long getClientGroupMid() {
        return clientGroupMid;
    }

    public void setClientGroupMid(Long clientGroupMid) {
        this.clientGroupMid = clientGroupMid;
    }

    public List<AccountProfile> getAccountProfiles() {
        return accountProfiles;
    }

    public void setAccountProfiles(List<AccountProfile> accountProfiles) {
        this.accountProfiles = accountProfiles;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
