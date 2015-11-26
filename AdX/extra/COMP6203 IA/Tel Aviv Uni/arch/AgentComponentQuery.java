package arch;

import edu.umich.eecs.tac.props.Query;

/**
 * All functionality components manage a list of query info. AgentComponentQuery
 * is the base class for such queries managed by the functionality components:
 * all contain a generic query object and implement a call-back whenever a new day is announced
 * 
 * @author Mariano Schain
 */
public abstract class AgentComponentQuery {
	protected Query query;
	
	public AgentComponentQuery(Query q) {
		query = new Query(q.getManufacturer(),q.getComponent());
	}
	
	public void nextDay(int day) {
	}
	
	public Query getQuery() {
		return query;
	}
	
	public void simulationFinished(){
		
	}
}
