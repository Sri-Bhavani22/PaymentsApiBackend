package com.payment.orchestration.dto;

import com.payment.orchestration.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new payment.
 * Contains all necessary fields for initiating a payment transaction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    /**
     * Payment amount (must be positive)
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Invalid amount format")
    private BigDecimal amount;

    /**
     * Currency code (e.g., INR, USD)
     */
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    private String currency;

    /**
     * Payment method (CARD, UPI, NET_BANKING, WALLET)
     */
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    /**
     * Card number (required for CARD payments)
     */
    @Size(min = 13, max = 19, message = "Invalid card number")
    private String cardNumber;

    /**
     * Card expiry date in MM/YY format (required for CARD payments)
     */
    @Pattern(regexp = "^(0[1-9]|1[0-2])/([0-9]{2})$", message = "Expiry date must be in MM/YY format")
    private String expiryDate;

    /**
     * Card CVV (required for CARD payments)
     */
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3 or 4 digits")
    private String cvv;

    /**
     * UPI ID (required for UPI payments)
     */
    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]+@[a-zA-Z]+$", message = "Invalid UPI ID format")
    private String upiId;

    /**
     * Customer email address
     */
    @Email(message = "Invalid email format")
    private String customerEmail;

    /**
     * Customer phone number
     */
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    private String customerPhone;

    /**
     * Payment description
     */
    @Size(max = 500, message = "Description too long")
    private String description;

    /**
     * Additional metadata as JSON string
     */
    private String metadata;

    /**
     * Validate payment method specific fields
     */
    public boolean isValid() {
        if (paymentMethod == PaymentMethod.CARD) {
            return cardNumber != null && !cardNumber.isBlank() &&
                   expiryDate != null && !expiryDate.isBlank() &&
                   cvv != null && !cvv.isBlank();
        }
        if (paymentMethod == PaymentMethod.UPI) {
            return upiId != null && !upiId.isBlank();
        }
        return true;
    }

    /**
     * Get validation error message for payment method specific fields
     */
    public String getValidationError() {
        if (paymentMethod == PaymentMethod.CARD) {
            if (cardNumber == null || cardNumber.isBlank()) {
                return "Card number is required for CARD payments";
            }
            if (expiryDate == null || expiryDate.isBlank()) {
                return "Expiry date is required for CARD payments";
            }
            if (cvv == null || cvv.isBlank()) {
                return "CVV is required for CARD payments";
            }
        }
        if (paymentMethod == PaymentMethod.UPI) {
            if (upiId == null || upiId.isBlank()) {
                return "UPI ID is required for UPI payments";
            }
        }
        return null;
    }
}
