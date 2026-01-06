package com.eazybytes.accounts.service.Client;

import com.eazybytes.accounts.DTO.CardsDto;
import com.eazybytes.accounts.DTO.LoansDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class CardsFallback implements CardsFeignClient {
    
    @Override
    public ResponseEntity<CardsDto> fetchCardDetails(String correlationId, String mobileNumber) {
        return null;
    }
}
