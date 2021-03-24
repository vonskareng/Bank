// Rebecka Skareng

package paradis.assignment2;


import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Använder mig av enkla reentrantlock och inte av readwrite locks
 * då readwrite locks inte är mer effektiva ifall läsning sker betydligt
 * oftare än skrivning och det framgår inte i uppgiften att så är fallet.
 */
@ThreadSafe
class Bank {
	// Instance variables.
	private final List<Account> accounts = new ArrayList<Account>();
	private final HashMap<Integer, ReentrantLock> locks = new HashMap<>(); //Håller reda på vilket lås som hör till ett visst konto genom dess id.
	private final ReentrantLock createAccountLock = new ReentrantLock(); //Lås för skapande av nya konton för att undvika överskivanden.
	// Instance methods.


	/**
	 * Låser skapandet av kontot med det instansens enskilda
	 * låsobjekt för att undvika att det blir konflikt vid 
	 * flera trådar.
	 * @param balance
	 * @return
	 */
	int newAccount(int balance) {
		int accountId;
		createAccountLock.lock();
		try{
			accountId = accounts.size(); // FIX ORIGINAL 
			accounts.add(new Account(accountId, balance)); 
			locks.put(accountId, new ReentrantLock());
	
			return accountId;
		}finally{
			createAccountLock.unlock();
		}

	}

	/**
	 * Hämtar låser som hör till accountId och låser 
	 * läsningen av kontots balance.
	 * @param accountId kontots id
	 * @return kontots balance
	 */
	int getAccountBalance(int accountId) {

		Account account = null;
		int balance;
		locks.get(accountId).lock();
		try {
			account = accounts.get(accountId);
			balance = account.getBalance();
			return balance;
		} finally {
			locks.get(accountId).unlock();
		}

	}

	/**
	 * Hämtar låset för det konto som operationen ska utföras på
	 * och låser skrivningen för det aktuella kontot.
	 * @param operation operationen som ska köras.
	 */
	void runOperation(Operation operation) {

		Account account = null;
		account = accounts.get(operation.getAccountId());
		locks.get(account.getId()).lock();
		try {
			int balance = account.getBalance();
			balance = balance + operation.getAmount();
			account.setBalance(balance);

		} finally {
			locks.get(account.getId()).unlock();
		}

	}

	/**
	 * Hämtar idn för konton som ska köras i transaktionen, för varje
	 * konto id försöker att hämta låset och låsa. Ifall Låset inte går att
	 * låsa alla lås för konton som ska köras så väntar tråden en randomiserad tid
	 * och försöker igen (100 gånger). Ifall tråden kan låsa alla nödvändiga lås så
	 * utförs operationerna i transaktionen och returnerar.
	 * @param transaction transaktionen som ska utföras
	 */
	void runTransaction(Transaction transaction) {

		Random random = new Random();
		for (int i = 0; i < 100; i++) {

			List<Operation> currentOperations = transaction.getOperations();
			List<Integer> accountIds = transaction.getAccountIds();
			List<ReentrantLock> lockedLocks = new ArrayList<>();

			for (Integer id : accountIds) {
				ReentrantLock lock = locks.get(id);
				if (lock.tryLock()) {
					lockedLocks.add(lock);
				}
			}

			try {
				if (lockedLocks.size() == accountIds.size()) {
					for (Operation operation : currentOperations) {
						runOperation(operation);
					}
					return;
				}
			} finally {
				for (ReentrantLock lock : lockedLocks) {
					lock.unlock();
				}
			}

			try {
				Thread.sleep(random.nextInt(100));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}


}
