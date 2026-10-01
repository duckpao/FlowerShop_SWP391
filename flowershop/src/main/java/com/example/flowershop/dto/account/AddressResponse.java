package com.example.flowershop.dto.account;

import com.example.flowershop.entity.Address;

public record AddressResponse(String id, String addressLine, String city, String district, String ward, String ghnWardCode, Integer ghnDistrictId, boolean isDefault) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getId(), a.getAddressLine(), a.getCity(), a.getDistrict(), a.getWard(), a.getGhnWardCode(), a.getGhnDistrictId(), Boolean.TRUE.equals(a.getIsDefault()));
    }
}
