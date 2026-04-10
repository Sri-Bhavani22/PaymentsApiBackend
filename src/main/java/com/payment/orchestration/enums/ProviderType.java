package com.payment.orchestration.enums;

/**
 * Payment provider types supported by the orchestration system.
 */
public enum ProviderType {
    
    /**
     * Provider A - Primary provider for CARD and NET_BANKING
     */
    PROVIDER_A("Provider A", true),
    
    /**
     * Provider B - Primary provider for UPI and WALLET
     */
    PROVIDER_B("Provider B", true);
    
    private final String displayName;
    private final boolean enabled;
    
    ProviderType(String displayName, boolean enabled) {
        this.displayName = displayName;
        this.enabled = enabled;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Get the failover provider for this provider
     */
    public ProviderType getFailoverProvider() {
        return this == PROVIDER_A ? PROVIDER_B : PROVIDER_A;
    }
}
