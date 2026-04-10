package com.payment.orchestration.enums;

/**
 * Supported payment methods in the orchestration system.
 */
public enum PaymentMethod {
    
    /**
     * Credit/Debit card payments - Routes to Provider A
     */
    CARD("Card Payment", "PROVIDER_A"),
    
    /**
     * UPI payments - Routes to Provider B
     */
    UPI("UPI Payment", "PROVIDER_B"),
    
    /**
     * Net Banking payments - Routes to Provider A
     */
    NET_BANKING("Net Banking", "PROVIDER_A"),
    
    /**
     * Wallet payments - Routes to Provider B
     */
    WALLET("Wallet Payment", "PROVIDER_B");
    
    private final String displayName;
    private final String primaryProvider;
    
    PaymentMethod(String displayName, String primaryProvider) {
        this.displayName = displayName;
        this.primaryProvider = primaryProvider;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getPrimaryProvider() {
        return primaryProvider;
    }
    
    /**
     * Check if this payment method requires card details
     */
    public boolean requiresCardDetails() {
        return this == CARD;
    }
    
    /**
     * Check if this payment method requires UPI ID
     */
    public boolean requiresUpiId() {
        return this == UPI;
    }
}
