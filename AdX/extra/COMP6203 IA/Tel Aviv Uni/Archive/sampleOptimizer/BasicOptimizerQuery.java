package sampleOptimizer;

/**
 * Query info for a BasicOptimizer
 * Each query maintains its score and computes related daily allocation 
 *
 * @author Mariano Schain
 */

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Product;
import edu.umich.eecs.tac.props.Query;
import arch.AgentComponentQuery;

public class BasicOptimizerQuery extends AgentComponentQuery {
	
	/* configuration parameters */
	protected  double alpha;
    protected  double etta;
    protected  int minBidUnits;
    protected  double spareKeepRatio;
    protected  double spareReturnRatio;
    
    
    /* attributes */
	public Ad ad;
	public Product product;
	public double bid;
	public double limit;
	public double bidUnits;
	
	/* scoring-related variables */
	public double wQ;
	public double wVpus;
	public Boolean takePart;
	public static double totalWq;
	
	/* latest data reported for yday */
    public double yRevenue;
    public int yConversions;
    public double yCost;
	
    /* latest estimates */
    protected double estImpressions;
    protected double estCpc;
    protected double estConversions; 
    protected double estClicks;
    protected double estProfits;
    
	public BasicOptimizerQuery(Query q) {
		super(q);
		
		product = new Product();
		ad = new Ad();
		
		if ((q.getManufacturer() != null) && (q.getComponent()!= null)) { /* Targeted Ad */
			product.setManufacturer(q.getManufacturer());
			product.setComponent(q.getComponent());
			ad.setProduct(product);
		} else { /* generic Ad */
			ad.setProduct(null);						
		}

	}
	
	/*
	 * populate values from configuration file
	 */
	public void setConfig(double b, double a, double e, int m, double sk, double sr) {
		bid = b;
		alpha = a;
		etta = e;
		minBidUnits = m;
		spareKeepRatio = sk;
		spareReturnRatio = sr;       	
	}
	
	public void setEstimates(double i, double c, double cv, double cl, double p) {
	    estImpressions=i;
	    estCpc=c;
	    estConversions=cv; 
	    estClicks=cl;
	    estProfits=p;
	 }
	
	/*
	 * the score of the query is based on the profits per unit sold
	 */
	public void computeScore() {
    	double vpus;
    	
		if ((yCost != 0) && (yConversions !=0)) 	
    		vpus = (yRevenue-yCost)/yConversions; 
    	else
    		vpus = 0.0;       
       	
       	wVpus = (1-alpha)*wVpus+alpha*vpus;       
        
    	wQ = Math.exp(etta*wVpus);
	
		if (wQ < 1) 
			takePart = false;
		else
			totalWq = totalWq + wQ;	
	}
	
	public Ad getAd() {
		return ad;
	}
	public Product getProduct() {
		return product;
	}
	public double getBid() {
		return bid;
	}
	
	public void nextDay(int day) {
		takePart = true;
		totalWq = 0.0;
		limit = 0.0;
		bidUnits = minBidUnits;
	}
/**
 * called by optimizer to allocate units to the query
 * @param quota the current available units quota
 * @param tw current round total score: the query allocation portion is its score divided by tw
 * @param capacity total items in capacity window. may be used to fine tune spare ratios (see below) 
 * @return spare to be reallocated in subsequent round
 */
	protected double addAllocation(double quota, double tw, int capacity) { 			
   		if (!takePart) 
   			return 0;
   		   		
   		double allocation = (wQ/tw)*quota;
   		double spare = 0.0;
   		double spareKept = 0.0;
   		double spareReturn = 0.0;
   		
		spare = bidUnits + allocation - estConversions;
    		
   		if (spare > 0) {
   			takePart = false; /* no need to consider in future allocation rounds for the day */
   			totalWq = totalWq - wQ;
   			
			spareKept = spareKeepRatio*spare;
   	   		spareReturn = spareReturnRatio*spare; 					

   			bidUnits = estConversions + spareKept;	 	  			
   			return spareReturn; 								
   		
   		} else {
   			/* all allocation used - no spares  */
   			/* wait for more in next allocation round */
   			bidUnits = bidUnits + allocation;
   			return 0;
   		}
   		  		
   	}

	protected void calculateBidInfo() { 			
		limit = bidUnits*estCpc*estClicks/estConversions;
    }
	
}
