package com.eazybytes.loans.exception;

public class ResourceNotAvailableException extends RuntimeException{

    public ResourceNotAvailableException(String resourceName,String fieldName,String fieldValue){
        super(String.format("%s Not found with %s : %s",resourceName,fieldName,fieldValue));
    }
}
