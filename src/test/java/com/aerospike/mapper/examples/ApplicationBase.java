package com.aerospike.mapper.examples;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.aerospike.mapper.examples.model.Address;
import com.aerospike.mapper.examples.model.Branch;
import com.aerospike.mapper.examples.model.Checkbook;
import com.aerospike.mapper.examples.model.Customer;
import com.aerospike.mapper.examples.model.InterestType;
import com.aerospike.mapper.examples.model.Property;
import com.aerospike.mapper.examples.model.Valuation;
import com.aerospike.mapper.examples.model.accounts.Account;
import com.aerospike.mapper.examples.model.accounts.AccountType;
import com.aerospike.mapper.examples.model.accounts.LoanAccount;
import com.aerospike.mapper.examples.model.accounts.PortfolioAccount;

public class ApplicationBase {
	protected Customer createAndPopulateCustomer() {
		Customer customer = new Customer("cust1", "Robert", "Smith");
		customer.setDateOfBirth(new Date(new Date().getTime() - TimeUnit.MILLISECONDS.convert(30*365, TimeUnit.DAYS)));
		customer.setPhone("(555)555-1234");
		customer.setPreferredSalutation("Bobby");
		customer.setJoinedBank(new Date());
		customer.setVip(true);
		return customer;
	}

	protected Account createAndPopulateChecking(String customerId) {
		Account checkingAccount = new Account("ACC-1234", customerId, "Checking Account", AccountType.CHECKING);
		checkingAccount.setBalance(100000);
		checkingAccount.setCard(true);
		checkingAccount.setRouting("123456789");
		checkingAccount.setPaperless(true);
		checkingAccount.setOverdraftProtection(false);
		checkingAccount.setOnlineUserName("beesmith");
		checkingAccount.setLastLogin(new Date());
		return checkingAccount;
	}
	
	protected Checkbook createAndPopulateCheckbook1(String accountId, Branch issuingBranch) {
		Checkbook checkbook = new Checkbook(accountId, 1, 100, new Date());
		checkbook.setIssuer(issuingBranch);
		checkbook.setRecalled(false);
		return checkbook;
	}

	protected Checkbook createAndPopulateCheckbook2(String accountId, Branch issuingBranch) {
		Checkbook checkbook = new Checkbook(accountId, 101, 600, new Date());
		checkbook.setIssuer(issuingBranch);
		checkbook.setRecalled(false);
		return checkbook;
	}
	
	protected Account createAndPopulateSavingsAccount(String customerId) {
		Account savingsAccount = new Account("SVG-999", customerId, "Savings Account", AccountType.SAVINGS);
		savingsAccount.setBalance(31415);
		savingsAccount.setCard(false);
		savingsAccount.setRouting("123456789");
		savingsAccount.setPaperless(false);
		savingsAccount.setOverdraftProtection(false);
		savingsAccount.setLastLogin(null);
		return savingsAccount;
	}
	
	protected PortfolioAccount createAndPopulatePortfolioAccount(String customerId) {
		PortfolioAccount portfolioAccount = new PortfolioAccount("PFOLIO-12312", customerId, "Portfolio Account", AccountType.PORTFOLIO);
		portfolioAccount.setBalance(314992);
		portfolioAccount.setCard(false);
		portfolioAccount.setRouting("134756700");
		portfolioAccount.setPaperless(false);
		portfolioAccount.setOverdraftProtection(false);
		portfolioAccount.setLastLogin(null);
		portfolioAccount.setContractClausesExcluded(37,108,312,333);
		return portfolioAccount;
	}
	
	protected LoanAccount createAndPopulateLoanAccount(String customerId) {
		Date originationDate = new Date(new Date().getTime() - TimeUnit.MILLISECONDS.convert(5*365, TimeUnit.DAYS));
		Date expirationDate = new Date(originationDate.getTime() + TimeUnit.MILLISECONDS.convert(30*365, TimeUnit.DAYS));
		LoanAccount loanAccount = new LoanAccount("LOAN-34672", customerId, "Loan Account", AccountType.LOAN, originationDate, expirationDate, 0.0399f);
		loanAccount.setBalance(31415);
		loanAccount.setCard(false);
		loanAccount.setRouting("123456789");
		loanAccount.setPaperless(false);
		loanAccount.setOverdraftProtection(false);
		loanAccount.setLastLogin(null);
		loanAccount.setInterestType(InterestType.PRINCIPAL_INTEREST);
		return loanAccount;
	}
	
	protected Property createAndPopulateProperty1() {
		return new Property(11567723, new Address("888 Yam Road", null, "Chicago", "IL", "55555"), 1977, "Stucco");
	}

	protected Property createAndPopulateProperty2() {
		Property property = new Property(11567724, new Address("333 Bob Jones Street", null, "Seattle", "WA", "23152"), 1955, "Brick");
		property
			.addValuation(new Valuation(new Date(), 550000, 4000, "Don's Valuers", 56367, new Address("67 Dartmouth Cl", null, "Chicago", "IL", "43252")))
			.addValuation(new Valuation(new Date(), 563000, 3000, "Tim's Valuers", 34245, null));
		return property;
	}

	protected Property createAndPopulateProperty3() {
		return new Property(11567725, new Address("14003 Des Moines Blvd", null, "Austin", "TX", "78123"), 2004, "Wood");
	}

	protected Property createAndPopulateProperty4() {
		Property property = new Property(8898, new Address("203 S Aneky St", null, "Denver", "CO", "82222-4321"), 1965, "Brick");
		property.addValuation(new Valuation(new Date(), 240000, 5000, "Steve's Valuers", 5647328, null))
				.addValuation(new Valuation(new Date(), 220000, 7000, "John's Valuers", 65462, new Address("88 Dartmouth Cl", null, "Smithfield", "IL", "43252")))
				.addValuation(new Valuation(new Date(), 255000, 3000, "Wilma's Valuers", 254665, null));

		return property;
	}
}
