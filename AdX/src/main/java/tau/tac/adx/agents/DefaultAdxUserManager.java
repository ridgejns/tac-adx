/*
 * DefaultUserManager.java
 *
 * COPYRIGHT  2008
 * THE REGENTS OF THE UNIVERSITY OF MICHIGAN
 * ALL RIGHTS RESERVED
 *
 * PERMISSION IS GRANTED TO USE, COPY, CREATE DERIVATIVE WORKS AND REDISTRIBUTE THIS
 * SOFTWARE AND SUCH DERIVATIVE WORKS FOR NONCOMMERCIAL EDUCATION AND RESEARCH
 * PURPOSES, SO LONG AS NO FEE IS CHARGED, AND SO LONG AS THE COPYRIGHT NOTICE
 * ABOVE, THIS GRANT OF PERMISSION, AND THE DISCLAIMER BELOW APPEAR IN ALL COPIES
 * MADE; AND SO LONG AS THE NAME OF THE UNIVERSITY OF MICHIGAN IS NOT USED IN ANY
 * ADVERTISING OR PUBLICITY PERTAINING TO THE USE OR DISTRIBUTION OF THIS SOFTWARE
 * WITHOUT SPECIFIC, WRITTEN PRIOR AUTHORIZATION.
 *
 * THIS SOFTWARE IS PROVIDED AS IS, WITHOUT REPRESENTATION FROM THE UNIVERSITY OF
 * MICHIGAN AS TO ITS FITNESS FOR ANY PURPOSE, AND WITHOUT WARRANTY BY THE
 * UNIVERSITY OF MICHIGAN OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT
 * LIMITATION THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE REGENTS OF THE UNIVERSITY OF MICHIGAN SHALL NOT BE LIABLE FOR ANY
 * DAMAGES, INCLUDING SPECIAL, INDIRECT, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, WITH
 * RESPECT TO ANY CLAIM ARISING OUT OF OR IN CONNECTION WITH THE USE OF THE SOFTWARE,
 * EVEN IF IT HAS BEEN OR IS HEREAFTER ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */
package tau.tac.adx.agents;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Message;
import tau.tac.adx.users.AdxUser;
import tau.tac.adx.users.generators.SimpleUserGenerator;
import edu.umich.eecs.tac.props.Auction;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.RetailCatalog;
import edu.umich.eecs.tac.props.UserClickModel;
import edu.umich.eecs.tac.sim.Auctioneer;
import edu.umich.eecs.tac.user.DefaultUsersInitializer;
import edu.umich.eecs.tac.user.QueryState;
import edu.umich.eecs.tac.user.User;
import edu.umich.eecs.tac.user.UserEventListener;
import edu.umich.eecs.tac.user.UserManager;
import edu.umich.eecs.tac.user.UserQueryManager;
import edu.umich.eecs.tac.user.UserTransitionManager;
import edu.umich.eecs.tac.user.UserViewManager;
import edu.umich.eecs.tac.user.UsersInitializer;

/**
 * @author Patrick Jordan, Ben Cassell, Lee Callender
 */
public class DefaultAdxUserManager implements UserManager {
	protected Logger log = Logger.getLogger(DefaultAdxUserManager.class
			.getName());

	private final Object lock;

	private final List<AdxUser> users;

	private final Random random;

	private final RetailCatalog retailCatalog;

	private final UserQueryManager queryManager;

	private final UserTransitionManager transitionManager;

	private final UserViewManager viewManager;

	private UserClickModel userClickModel;

	private final UsersInitializer usersInitializer;

	public DefaultAdxUserManager(RetailCatalog retailCatalog,
			UserTransitionManager transitionManager,
			UserQueryManager queryManager, UserViewManager viewManager,
			int populationSize) {
		this(retailCatalog, transitionManager, queryManager, viewManager,
				populationSize, new Random());
	}

	public DefaultAdxUserManager(RetailCatalog retailCatalog,
			UserTransitionManager transitionManager,
			UserQueryManager queryManager, UserViewManager viewManager,
			int populationSize, Random random) {
		lock = new Object();

		if (retailCatalog == null) {
			throw new NullPointerException("Retail catalog cannot be null");
		}

		if (transitionManager == null) {
			throw new NullPointerException(
					"User transition manager cannot be null");
		}

		if (queryManager == null) {
			throw new NullPointerException("User query manager cannot be null");
		}

		if (viewManager == null) {
			throw new NullPointerException("User view manager cannot be null");
		}

		if (populationSize < 0) {
			throw new IllegalArgumentException(
					"Population size cannot be negative");
		}

		if (random == null) {
			throw new NullPointerException(
					"Random number generator cannot be null");
		}

		this.retailCatalog = retailCatalog;
		this.random = random;
		this.transitionManager = transitionManager;
		this.queryManager = queryManager;
		this.viewManager = viewManager;
		this.usersInitializer = new DefaultUsersInitializer(transitionManager);
		SimpleUserGenerator generator = new SimpleUserGenerator();
		users = generator.generate(populationSize);
	}

	@Override
	public void initialize(int virtualDays) {
		usersInitializer.initialize(users, virtualDays);
	}

	@Override
	public void triggerBehavior(Auctioneer auctioneer) {

		synchronized (lock) {
			log.finest("START OF USER TRIGGER");

			Collections.shuffle(users, random);

			for (User user : users) {

				boolean transacted = handleSearch(user, auctioneer);

				handleTransition(user, transacted);
			}

			log.finest("FINISH OF USER TRIGGER");
		}

	}

	private boolean handleSearch(User user, Auctioneer auctioneer) {

		boolean transacted = false;

		Query query = generateQuery(user);

		if (query != null) {
			Auction auction = auctioneer.runAuction(query);

			transacted = handleImpression(query, auction, user);
		}

		return transacted;
	}

	private boolean handleImpression(Query query, Auction auction, User user) {
		return viewManager.processImpression(user, query, auction);
	}

	private void handleTransition(User user, boolean transacted) {
		user.setState(transitionManager.transition(user, transacted));
	}

	private Query generateQuery(User user) {
		return queryManager.generateQuery(user);
	}

	@Override
	public boolean addUserEventListener(UserEventListener listener) {
		synchronized (lock) {
			return viewManager.addUserEventListener(listener);
		}
	}

	@Override
	public boolean containsUserEventListener(UserEventListener listener) {
		synchronized (lock) {
			return viewManager.containsUserEventListener(listener);
		}
	}

	@Override
	public boolean removeUserEventListener(UserEventListener listener) {
		synchronized (lock) {
			return viewManager.removeUserEventListener(listener);
		}
	}

	@Override
	public void nextTimeUnit(int timeUnit) {
		viewManager.nextTimeUnit(timeUnit);
		queryManager.nextTimeUnit(timeUnit);
		transitionManager.nextTimeUnit(timeUnit);
	}

	@Override
	public int[] getStateDistribution() {
		int[] distribution = new int[QueryState.values().length];

		for (User user : users) {
			distribution[user.getState().ordinal()]++;
		}

		return distribution;
	}

	@Override
	public int[] getStateDistribution(Product product) {
		int[] distribution = new int[QueryState.values().length];

		for (User user : users) {
			if (user.getProduct() == product) {
				distribution[user.getState().ordinal()]++;
			}
		}

		return distribution;
	}

	@Override
	public RetailCatalog getRetailCatalog() {
		return this.retailCatalog;
	}

	@Override
	public UserClickModel getUserClickModel() {
		return userClickModel;
	}

	@Override
	public void setUserClickModel(UserClickModel userClickModel) {
		this.userClickModel = userClickModel;
		viewManager.setUserClickModel(userClickModel);
	}

	@Override
	public void messageReceived(Message message) {
		Transportable content = message.getContent();

		if (content instanceof UserClickModel) {
			setUserClickModel((UserClickModel) content);
		}
	}
}
