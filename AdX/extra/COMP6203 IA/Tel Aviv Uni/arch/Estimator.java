package arch;

/**
 * An Estimator implements the IEstimator interface and maintains reference to a Modeler
 * 
 * @author Mariano Schain
 */
public abstract class Estimator extends AgentComponent implements IEstimator {
	
	protected IModeler aaModeler;
	
	public void setModeler(IModeler m) {
		aaModeler = m;
	}

}
