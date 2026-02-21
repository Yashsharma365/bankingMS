package com.eazybytes.accounts.service.Impl;

import com.eazybytes.accounts.Constants.Constants;
import com.eazybytes.accounts.DTO.AccountsDto;
import com.eazybytes.accounts.DTO.AccountsMsgDto;
import com.eazybytes.accounts.DTO.CustomerDto;
import com.eazybytes.accounts.entity.Accounts;
import com.eazybytes.accounts.entity.Customer;
import com.eazybytes.accounts.exception.CustomerAlreadyExistsException;
import com.eazybytes.accounts.exception.ResourceNotFoundException;
import com.eazybytes.accounts.mapper.AccountsMapper;
import com.eazybytes.accounts.mapper.CustomerMapper;
import com.eazybytes.accounts.repo.AccountsRepo;
import com.eazybytes.accounts.repo.CustomerRepo;
import com.eazybytes.accounts.service.IAccountsService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
public class AccountsServiceImpl implements IAccountsService {

    private static final Logger logger = LoggerFactory.getLogger(AccountsServiceImpl.class);

    private AccountsRepo accountsRepo;
    private CustomerRepo customerRepo;

    private final StreamBridge streamBridge;
    @Override
    public void createAccount(CustomerDto customerDTO) {
        Customer customer = CustomerMapper.mapToCustomers(customerDTO, new Customer());
        Optional<Customer> existingCustomer = customerRepo.findByMobileNumber(customer.getMobileNumber());
        if(existingCustomer.isPresent()){
            throw new CustomerAlreadyExistsException("It already here. Phone number here. New sim "
                    + customerDTO.getMobileNumber());
        }

        Customer savedCustomer = customerRepo.save(customer);
        Accounts savedAccount = accountsRepo.save(createNewAccount(savedCustomer));
        sendCommunication(savedAccount,savedCustomer);
    }

    private void sendCommunication(Accounts accounts,Customer customer){
        var accountsMsgDto = new AccountsMsgDto(accounts.getAccountNumber(),customer.getName(), customer.getEmail(), customer.getMobileNumber());
        logger.info("AccountsMsgDto: {}",accountsMsgDto);
        var result = streamBridge.send("sendCommunication-out-0",accountsMsgDto);
        logger.info("Communication request successfully processed? : {}",result);
    }

    @Override
    public CustomerDto fetchAccount(String mobileNumber) {
        Customer customer =customerRepo.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer","mobileNumber",mobileNumber)
        );
        Accounts accounts = accountsRepo.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account","CustomerId",customer.getCustomerId().toString())
        );

        CustomerDto customerDTO=CustomerMapper.mapToCustomerDto(customer,new CustomerDto());
        customerDTO.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts,new AccountsDto()));
        return customerDTO;
    }

    @Override
    public boolean updateAccount(CustomerDto customerDTO) {
        boolean isUpdated= false;
        AccountsDto accountsDto = customerDTO.getAccountsDto();
        if(accountsDto !=null) {
            Accounts accounts = accountsRepo.findById(accountsDto.getAccountNumber()).orElseThrow(
                    () -> new ResourceNotFoundException("Account", "AccountNumber", accountsDto.getAccountNumber().toString())
            );
            AccountsMapper.mapToAccounts(accountsDto, accounts);
            accounts= accountsRepo.save(accounts);

            Long customerId = accounts.getCustomerId();
            Customer customer= customerRepo.findById(customerId).orElseThrow(
                    () -> new ResourceNotFoundException("Customer", "CustomerId", customerId.toString())
            );
            customerRepo.save(customer);
            isUpdated=true;
        }
        return isUpdated;
    }

    /**
     * @param mobileNumber - Input Mobile Number
     * @return boolean indicating if the delete of Account details is successful or not
     */
    @Override
    public boolean deleteAccount(String mobileNumber) {
        Customer customer = customerRepo.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
        );
        accountsRepo.deleteByCustomerId(customer.getCustomerId());
        customerRepo.deleteById(customer.getCustomerId());
        return true;
    }


    /**
                 * @param customer - Customer Object
                 * @return the new account details
                 */
    private Accounts createNewAccount(Customer customer) {
        Accounts newAccount = new Accounts();
        newAccount.setCustomerId(customer.getCustomerId());
        long randomAccNumber = 1000000000L + new Random().nextInt(900000000);

        newAccount.setAccountNumber(randomAccNumber);
        newAccount.setAccountType(Constants.SAVINGS);
        newAccount.setBranchAddress(Constants.ADDRESS);
        return newAccount;
    }

    @Override
    public boolean updateCommunication(Long accountNumber) {
        boolean isUpdated=false;

        if(accountNumber!=null){
            Accounts accounts = accountsRepo.findById(accountNumber).orElseThrow(
                    () -> new ResourceNotFoundException("Account","AccountNumber",accountNumber.toString())
            );
            accounts.setCommunicationSw(true);
            accountsRepo.save(accounts);
            isUpdated=true;
        }
        return isUpdated;

    }

}
