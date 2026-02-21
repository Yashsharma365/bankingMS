package com.eazybytes.accounts.service;

import com.eazybytes.accounts.DTO.CustomerDetailsDto;
import org.apache.commons.lang3.ClassUtils;

public interface ICustomersService {

    CustomerDetailsDto fetchCustomerDetails(String mobileNumber, String correlationId);
}
