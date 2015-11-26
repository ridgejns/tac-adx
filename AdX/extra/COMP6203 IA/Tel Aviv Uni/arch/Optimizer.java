package arch;

/**
 * Abstract Optimizer. Base class for any optimizer component: 
 * 
 * must implement the IOptimizer interface!
 * maintains a reference to an Estimator
 *
 * @author Mariano Schain
 */
public abstract class Optimizer extends AgentComponent implements IOptimizer {
	
	protected IEstimator aaEstimator;
	
	public void setEstimator(IEstimator e) {
		aaEstimator = e;
	}
}
