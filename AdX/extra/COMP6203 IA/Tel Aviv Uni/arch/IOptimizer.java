package arch;

import edu.umich.eecs.tac.props.*;

/**
 * This interface is implemented by any Optimizer
 * The optimize method is called by the agent and the returned 
 * BidBundle is sent to the simulation server as the bid of the agent for
 * the next day
 *
 * @author Mariano Schain
 */

public interface IOptimizer {
	BidBundle optimize();
}
