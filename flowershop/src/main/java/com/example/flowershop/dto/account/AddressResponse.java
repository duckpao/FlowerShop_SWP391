package com.example.flowershop.dto.account;

import com.example.flowershop.entity.Address;

public record AddressResponse(String id, String addressLine, String city, String district, String ward, boolean isDefault,
        String recipientName, String recipientPhone, boolean isPickup, boolean isReturn, String addressType,
        String ghnWardCode, Integer ghnDistrictId) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getId(), a.getAddressLine(), a.getCity(), a.getDistrict(), a.getWard(), Boolean.TRUE.equals(a.getIsDefault()),
                a.getRecipientName(), a.getRecipientPhone(), Boolean.TRUE.equals(a.getIsPickup()), Boolean.TRUE.equals(a.getIsReturn()), a.getAddressType(), a.getGhnWardCode(), a.getGhnDistrictId());
    }
}
