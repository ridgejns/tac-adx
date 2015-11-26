package sampleModeler;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;



import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import arch.Modeler;

public class BasicModeler extends Modeler {

	double decay;
    protected static double DECAY_DEFAULT = 0.1;

	protected Queue<BasicModelerQuery> querySpace;
	
	public void simulationReady() {
		Set<Query> querySet = aaAgent.getQuerySet();
		
		for(Query query : querySet) { 
        	querySpace.add(new BasicModelerQuery(query));        	
        }
	}
	
	public void nextDay(int day) {
		for(BasicModelerQuery query : querySpace) { 
        	query.nextDay(day);        	
        }
	}

	
	public BasicModeler() {
		querySpace = new LinkedList<BasicModelerQuery>();
	}
	
	public void handleQueryReport(QueryReport queryReport, int yday) {
		for(BasicModelerQuery query : querySpace) {      	
        	query.cpc[yday] = queryReport.getCPC(query.getQuery());
        	query.impressions[yday] = queryReport.getImpressions(query.getQuery());
        	
        	/* update estimates */
        	if (!query.cpc[yday].isNaN()) 
        		query.estCpc[yday+2] = decay*query.cpc[yday] + (1-decay)*query.estCpc[yday+1];
        	else
        		query.estCpc[yday+2] = query.estCpc[yday+1];
        	
        	query.estImpressions[yday+2] = decay*query.impressions[yday] + (1-decay)*query.estImpressions[yday+1];
       	}
	}

	public void handleSalesReport(SalesReport salesReport, int yday) {}
	
	public ModelerResult model(Query query, double bid, Ad ad, int day) {
		ModelerResult result = new ModelerResult(0.0,0.0,0.0);
		
		for(BasicModelerQuery mquery : querySpace) {      	
			if (mquery.getQuery().equals(query)) {
				result.setImpressions(mquery.estImpressions[day]);
				result.setCpc(mquery.estCpc[day]);
			}
		}
		return result;
	}
	
	public void simulationFinished() {
		querySpace.clear();		
	}
	
	public void simulationSetup() {
		decay = aaConfig.getPropertyAsDouble("Decay", DECAY_DEFAULT);
	}
}
