package sampleModeler;

import static arch.AgentConstants.TAU_SIMDAYS;
import edu.umich.eecs.tac.props.Query;
import arch.AgentComponentQuery;

public class BasicModelerQuery extends AgentComponentQuery {

	/* estimated */
	public Double 	[]	estCpc;
	public Double 	[]	estImpressions;
	
    /* actuals */
	public Double	[]	cpc;
	public int 		[]	impressions;		

	/* initial defaults */
    protected static double CPC_INIT = 2.0;
    protected static double IMPR_INIT = 100.0;
	
	
	public BasicModelerQuery(Query q) {
		super(q);
	
		estCpc = 			new Double[TAU_SIMDAYS+3];
		estImpressions =	new Double[TAU_SIMDAYS+3];
		cpc = 				new Double[TAU_SIMDAYS+3];
		impressions = 		new int[TAU_SIMDAYS+3];
		
		nextDay(0);
	}
		
	
	
	/* prepare for the next day results 
	 * we initialize the estimates to default values
	 * @see arch.AgentComponentQuery#nextDay(int)
	 */
	public void nextDay(int day) {
		estCpc[day]=CPC_INIT;
		estImpressions[day]=IMPR_INIT;
	}

}
