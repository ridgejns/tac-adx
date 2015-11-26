package sampleOptimizer;

/**
 * Basic implementation of an optimizer
 * Ranks each query by a 'Regret Minimization' scheme (based on daily performance)
 * and allocated the capacity quota among the queries based on the rank. 
 *
 * @author Mariano Schain
 */

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;
import edu.umich.eecs.tac.props.QueryReport;
import edu.umich.eecs.tac.props.SalesReport;
import arch.Optimizer;
import arch.IEstimator.DayState;
import arch.IEstimator.QueryEstimateResult;

public class BasicOptimizer extends Optimizer {
	
	private static final Logger log = Logger.getLogger("optimizer");

	/*
	 * algorithm parameters
	 */
	protected int allocRounds;
    protected static int ALLOC_ROUNDS_DEFAULT = 3;
    
    protected double initialBid;
    protected static double INITIAL_BID_DEFAULT = 5.0;

    protected double initialLimit;
    protected static double INITIAL_LIMIT_DEFAULT = 20.0;

    protected double ALPHA_DEFAULT = 0.8;
    protected double ETTA_DEFAULT = 0.5;
    protected int MINBIDUNITS_DEFAULT = 3;
    protected double SPAREKEEPR_DEFAULT = 0.6;
    protected double SPARERETURNR_DEFAULT = 0.5;

    /*
     * quota calculation variables
     */
	protected int capacityUsed;
	protected int capacity;
    protected Queue<Integer> salesWindow;
    protected int capacityWindow;
    protected int capacityQuota; 
    protected int minDailySales;    
    protected int estimatedSales;
       
    /*
     * holds information for all relevant queries
     */
    protected Queue<BasicOptimizerQuery> querySpace;
	
	protected QueryEstimateResult estimated;

    protected BidBundle bidBundle;
    
    protected int day;

	public BasicOptimizer() {
		querySpace = new LinkedList<BasicOptimizerQuery>();		
		salesWindow = new LinkedList<Integer>();
		estimated = new QueryEstimateResult();
	}
	
	/*
	 * 
	 * @see arch.IAgentComponent#simulationReady()
	 */
	public void simulationReady() {
		Set<Query> querySet = aaAgent.getQuerySet();
								
		day = 0;
		
		/* create queries space */
		for(Query query : querySet) { 
        	querySpace.add(new BasicOptimizerQuery(query));        	
        }
		
		/*
		 * we populate the bidBundle once upon simulationReady and update upon optimize()
		 */
		bidBundle = new BidBundle();
		for(BasicOptimizerQuery optquery : querySpace) { 
        	bidBundle.addQuery(optquery.getQuery(),initialBid,optquery.getAd());
        	bidBundle.setDailyLimit(optquery.getQuery(), Double.NaN);
        	bidBundle.setCampaignDailySpendLimit(Double.NaN);
        	optquery.setConfig(
        			initialBid, 
        			aaConfig.getPropertyAsDouble("Alpha", ALPHA_DEFAULT), 
        			aaConfig.getPropertyAsDouble("Etta", ETTA_DEFAULT),
        			aaConfig.getPropertyAsInt("MinBidUnits", MINBIDUNITS_DEFAULT),
        			aaConfig.getPropertyAsDouble("SpareKeepRatio", SPAREKEEPR_DEFAULT),
        			aaConfig.getPropertyAsDouble("SpareReturnRatio", SPARERETURNR_DEFAULT)
        			);
        	optquery.nextDay(0);
        }
				
		capacity = aaAgent.getAdvertiserInfo().getDistributionCapacity();
		capacityWindow = aaAgent.getAdvertiserInfo().getDistributionWindow();
		capacityQuota = 0;
		minDailySales = capacity/(2*capacityWindow);

		log.log(Level.FINE, "Capacity: "+capacity);

	}
	
	
	public void nextDay(int d) {
		day = d;
		for(BasicOptimizerQuery query : querySpace) { 
        	query.nextDay(d);        	
        }
	}
	
	/*
	 * merely fetch the optimized info for each query 
	 * (as calculated upon reception of the the status messages)
	 * and update the return bidBundle accordingly
	 * @see arch.IOptimizer#optimize()
	 */
    public BidBundle optimize() {
		Query query;
		int tomorrow = day+1;
		double limit;
		
		log.log(Level.FINE," Bidding for day "+ tomorrow);		
		for(BasicOptimizerQuery optquery : querySpace) { 
			query = optquery.getQuery();
			
			/* limit is irrelevant until day 2 since no reports were received */
			if (tomorrow > 2) 
				limit = optquery.limit;	 
			else
				limit = initialLimit;	
			
			bidBundle.setBid(query, optquery.bid);	       	
			bidBundle.setAd(query, optquery.ad);
			bidBundle.setDailyLimit(query, limit);	 
			
			Double dlimit = limit;			
			Double du = optquery.bidUnits;
			Double dcl = optquery.estClicks;
			Double dcv = optquery.estConversions;
			double dcpc = (double)(((Double)(100*optquery.estCpc)).intValue())/100;						
			double dbid = (double)(((Double)(100*optquery.bid)).intValue())/100;
			
			log.log(Level.FINE, "("+query.getManufacturer()+","+query.getComponent()+")"+" bid: "+dbid+ ", limit: "+dlimit.intValue() + 
					" (u:"+ du.intValue() + " cl:"+ dcl.intValue() + " cv:" + dcv.intValue() + " cpc:"+ dcpc + ")");
        }
		
		bidBundle.setCampaignDailySpendLimit(Double.NaN);
    	     
        return bidBundle;
	}
	
 /**
  * the quota of items to sell tomorrow (tday) is determined based on past sales 
  * (including yesterday sales - yday).
  */
    public void handleSalesReport(SalesReport salesReport, int yday) {
     	
		int currentSales = 0;
        int estimatedCapacityUsed = 0;
        int today = yday+1;
        int tomorrow = yday +2;

		log.log(Level.FINE," Sales report for day "+ yday);		
		
        if (yday > 0) { /* sales reports are not expected for day 0 */
        	
        	for(BasicOptimizerQuery optquery : querySpace) {   
        		optquery.yConversions = salesReport.getConversions(optquery.getQuery());
        		optquery.yRevenue = salesReport.getRevenue(optquery.getQuery());
        		
    			log.log(Level.FINE, "("+ optquery.getQuery().getManufacturer() +","+optquery.getQuery().getComponent()+")"+" day "+yday + " reported sales: " +optquery.yConversions + ", revenue: "+optquery.yRevenue);
        		
            	currentSales = currentSales + optquery.yConversions;
        	}       	
        	
        	capacityUsed = capacityUsed + currentSales;
        	    		
        	salesWindow.add(currentSales);
        	/* keep sales of last (capacityWindow - 1) days */
        	if (salesWindow.size() > capacityWindow - 1) { 
        		capacityUsed = capacityUsed - salesWindow.poll();
        	} 
        	
        	/* 
        	 * today's sales estimation is the average of the Estimator's estimation
        	 * and the quota allocated yesterday
        	 */
        	estimatedSales = (estimatedSales+capacityQuota)/2;
        	
        	/* if late enough, deduct oldest day in capacity window from capacity usage estimation */
        	if (salesWindow.size() >= capacityWindow - 1) { 
        		estimatedCapacityUsed = capacityUsed + estimatedSales - salesWindow.peek();
        	} else {
        		estimatedCapacityUsed = capacityUsed + estimatedSales;        		
        	}
        	
        	capacityQuota = capacity - estimatedCapacityUsed;
        	
        	if (capacityQuota < minDailySales)
        		capacityQuota = minDailySales;
 
        }
        
		log.log(Level.FINE, "Day "+yday+" sales: "+currentSales + ", Day "+today+ " sales estimate: "+ estimatedSales);
		log.log(Level.FINE, "Day "+today+" estimated capacity used: "+estimatedCapacityUsed + "  Day "+tomorrow+ " capacity quota: "+capacityQuota);
	}
	
	
    public void handleQueryReport(QueryReport queryReport, int yday) {
    	double quota;
    	int round;
    	double totalWq;
    	double spareAllocation;
    	DayState tdaystate = new DayState();
    	
    	tdaystate.setDay(yday+2); /* given 'yesterday', we need estimates for 'tomorrow' */
    	
    	/* reset the estimation for tomorrow's sales - we need this figure for the quota calculations */
    	estimatedSales = 0;
		log.log(Level.FINE," Query report for day "+ yday);		

        if (yday > 0) {
        	/* first we score each query and ask the estimator for tomorrow's estimates */
           	for(BasicOptimizerQuery query : querySpace) {      	
            	query.yCost = queryReport.getCost(query.getQuery());
            	query.computeScore(); 
            	estimated = aaEstimator.estimateQuery(query.getQuery(),query.bid, query.ad, query.limit ,tdaystate);
            	
    			double dcpc = (double)(((Double)(100*queryReport.getCPC(query.getQuery()))).intValue())/100;						
      			log.log(Level.FINE, "("+query.getQuery().getManufacturer()+","+query.getQuery().getComponent()+")"+" day "+yday + " reported " +
    					" i:" + queryReport.getImpressions(query.getQuery())+
    					" cl:"+ queryReport.getClicks(query.getQuery())+
    					" cv:" + query.yConversions+
    					" cpc:"+ dcpc);

            	
            	query.setEstimates(
            	  	estimated.getImpressions(),
            	  	estimated.getCpc(),
            	  	estimated.getConversions(),
            	  	estimated.getClicks(),
            	  	estimated.getProfits());

            	estimatedSales = estimatedSales + (int)(query.estConversions);  	
           	}  	
         
        	/* next we calculate the allocation for each query, based on its score */ 
         	quota = capacityQuota;
         	round = 0;
        	
         	totalWq = BasicOptimizerQuery.totalWq;   	
         	
        	while ((quota > 0) && (round < allocRounds)) {
               	spareAllocation = 0;
                for(BasicOptimizerQuery query : querySpace) {      	        	
        			spareAllocation = spareAllocation + query.addAllocation(quota,totalWq,capacity);        	
        		}

                totalWq = BasicOptimizerQuery.totalWq;
               	quota = spareAllocation;
        		round = round + 1;
         	}        	

        	/* finally, we can calculate spend limits for the bid based on allocated units */ 
        	for(BasicOptimizerQuery query : querySpace) {      	        	
        		query.calculateBidInfo();       	
        	}
        }
    }	
	
    public void simulationFinished() {
    	querySpace.clear();		
        salesWindow.clear();
     }

    public void simulationSetup() {   	
    	allocRounds = aaConfig.getPropertyAsInt("AllocationRounds", ALLOC_ROUNDS_DEFAULT);
		initialBid = aaConfig.getPropertyAsDouble("Bid", INITIAL_BID_DEFAULT);
		initialLimit = aaConfig.getPropertyAsDouble("Limit", INITIAL_LIMIT_DEFAULT);
    }
    
}
