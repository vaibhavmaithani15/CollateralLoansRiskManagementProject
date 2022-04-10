package com.cts.training.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.cts.training.exception.CollateralTypeNotFoundException;
import com.cts.training.exception.CustomerLoanNotFoundException;
import com.cts.training.exception.LoanNotFoundException;
import com.cts.training.feign.CollateralFeign;
import com.cts.training.model.ApplyLoan;
import com.cts.training.model.CustomerLoan;
import com.cts.training.model.Loan;
import com.cts.training.pojo.CashDeposit;
import com.cts.training.pojo.RealEstate;
import com.cts.training.repo.ApplyLoanRepo;
import com.cts.training.repo.CustomerLoanRepo;
import com.cts.training.repo.LoanRepo;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

/**
 * LoanManagementService implementation
 */
@Service
@Slf4j
public class LoanManagementServiceImpl implements LoanManagementService {

	@Autowired
	private CollateralFeign client;

	@Autowired
	private CustomerLoanRepo customerLoanRepo;

	@Autowired
	private LoanRepo loanRepo;
	
	@Autowired
	private ApplyLoanRepo applicationRepo;

	private static final String MESSAGE = "Customer Loan Not found with LoanId: ";

	
	/**
	 * Get Loan Details Implimentation
	 */
	@Override
	public CustomerLoan getLoanDetails(int loanId, int customerId) throws CustomerLoanNotFoundException {
		log.info("Get Loan details using loan id and customer id");
		log.info(loanId+"======="+customerId);
		System.out.println("Inside loan management service================");
		CustomerLoan customerLoan = customerLoanRepo.findById(loanId)
		.orElseThrow(() -> new CustomerLoanNotFoundException(MESSAGE + loanId));
		/*
		 * Optional<CustomerLoan> customerLoan=customerLoanRepo.findById(loanId);
		 * System.out.println(customerLoan.get()); if(!customerLoan.isPresent()) { throw
		 * new CustomerLoanNotFoundException(MESSAGE+loanId); }
		 */
		System.out.println(customerLoan);
		if (customerLoan.getCustomerId() != customerId) {
			throw new CustomerLoanNotFoundException(MESSAGE + loanId);
		}
		return customerLoan;
	}
	
	/**
	 * Save RealEstate Implementatiom
	 * 
	 * @throws LoanNotFoundException
	 */
	@Override
	public ResponseEntity<String> saveRealEstate(String token, RealEstate realEstate)
			throws CustomerLoanNotFoundException, LoanNotFoundException {
		log.info("Save Real Estate collateral details");
		System.out.println("===========Saving Real Estate details============= from loan management service"+realEstate);
		CustomerLoan customerLoan = customerLoanRepo.findById(realEstate.getLoanId())
				.orElseThrow(() -> new CustomerLoanNotFoundException(MESSAGE + realEstate.getLoanId()));

		Integer prodId = customerLoan.getLoanProductId();
		Optional<Loan> loanop = loanRepo.findById(prodId);
		if(!loanop.isPresent()){
			throw new LoanNotFoundException("Loan Not found by Id" + prodId);
		}else{
			Loan loan = loanop.get();
			String type = loan.getCollateralType();
		try {
			if (type.equals("REAL_ESTATE")) {

				customerLoan.setCollateralId(realEstate.getCollateralId());
				customerLoanRepo.save(customerLoan);
				return client.saveRealEstateCollateral(token, realEstate);
			} else {
				throw new CollateralTypeNotFoundException("Collateral Mismatch");
			}
		} catch (FeignException e) {
			e.printStackTrace();
			throw new CollateralTypeNotFoundException("Collateral already exists with loan id");
		}
		}
	}
	
	/**
	 * Save Cash Deposit Implementation
	 * 
	 * @throws LoanNotFoundException
	 */
	@Override
	public ResponseEntity<String> saveCashDeposit(String token, CashDeposit cashDeposit)
			throws CustomerLoanNotFoundException, LoanNotFoundException {
		log.info("Save Cash Deposit collateral details");
		CustomerLoan customerLoan = customerLoanRepo.findById(cashDeposit.getLoanId())
				.orElseThrow(() -> new CustomerLoanNotFoundException(MESSAGE + cashDeposit.getLoanId()));

		Integer prodId = customerLoan.getLoanProductId();
		Optional<Loan> loanop = loanRepo.findById(prodId);
		if(!loanop.isPresent()){
			throw new LoanNotFoundException("Loan not Found By Id:" + prodId);
		}else{
			Loan loan = loanop.get();
			String type = loan.getCollateralType();
			try {
				if (type.equals("CASH_DEPOSIT")) {
					customerLoan.setCollateralId(cashDeposit.getCollateralId());
					customerLoanRepo.save(customerLoan);
					return client.saveCashDepositCollateral(token, cashDeposit);
				} else {
					throw new CollateralTypeNotFoundException("Collateral Mismatch");
				}
			} catch (FeignException e) {
				
				throw new CollateralTypeNotFoundException("Collateral already exists with loan id");
			}
		}
	}
	
	
	
	
	
	/**
	 *Database storage LoanApp
	 */
	@Override
	public ResponseEntity<String> applyLoan(ApplyLoan loanApplication) {
		applicationRepo.save(loanApplication);
		return new ResponseEntity<>("Application Saved", HttpStatus.ACCEPTED);
	}
	/**
	 * CustomerIf->List of Loans
	 */
	@Override
	public ArrayList<ApplyLoan> viewCustLoan(int custId) {
		ArrayList<ApplyLoan> list=new ArrayList<>();
		for(ApplyLoan application:applicationRepo.findAll()) {
			if(application.getCustId()==custId) {
				
				list.add(application);
			}
		}
		
		return list;
	}
	/**
	 *Accepted/Rejected cust return
	 */
	@Override
	public ArrayList<ApplyLoan> getAll(){
		ArrayList<ApplyLoan> list=new ArrayList<ApplyLoan>();
		for(ApplyLoan application:applicationRepo.findAll()) {
			if(!application.getStatus().equals("Accepted") && !application.getStatus().equals("Rejected"))
				list.add(application);
		}
		return list;
	}
	/**
	 * Add details to customerloan tab
	 */
	@Override
	public ResponseEntity<String> approveLoan(Integer applicationId){
		
		ApplyLoan application= applicationRepo.findById(applicationId).get();
		application.setStatus("Accepted");
		applicationRepo.save(application);
		
		
		CustomerLoan customerLoan=new CustomerLoan();
		Integer cId=0;
		if(application.getCollDetails().equalsIgnoreCase("Cash Deposit")) {
			cId=101;
		}
		else if(application.getCollDetails().equalsIgnoreCase("Real Estate")) {
			cId=102;
		}
		Double emi=(Double)application.getLoanAmnt()/12.0*application.getTenure();
		customerLoan.setCustomerId(application.getCustId());
		customerLoan.setLoanPrincipal(application.getLoanAmnt());
		customerLoan.setTenure(application.getTenure());
		customerLoan.setInterest(10.5);
		customerLoan.setEmi(emi);
		customerLoan.setCollateralId(cId);
		customerLoanRepo.save(customerLoan);
		customerLoan.setLoanProductId(customerLoan.getLoanId()+1000);
		customerLoanRepo.save(customerLoan);
		
		
		return new ResponseEntity<>("Loan Application Accepted", HttpStatus.ACCEPTED);
	
	}
	/**
	 * Rejecctt and update
	 */ 
	@Override
	public ResponseEntity<String> rejectLoan(Integer applicationId){
		
		ApplyLoan application=applicationRepo.findById(applicationId).get();
		application.setStatus("Rejected");
		applicationRepo.save(application);
		return new ResponseEntity<>("Loan Application Rejected", HttpStatus.ACCEPTED);
	
	}
	
	
}
