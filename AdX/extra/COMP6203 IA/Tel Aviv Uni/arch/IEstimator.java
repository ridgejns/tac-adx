package arch;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BidBundle;
import edu.umich.eecs.tac.props.Query;


/**
 * This interface is implemented by any Estimator
 *
 * @author Mariano Schain
 */
public interface IEstimator {

	public class DayState {
		int day; /* relevant day */
		int utilizedWindowCapacity; /* amount of capacity utilized at day start */
		int dailyUsedCapacity; /* amount of capacity to be utilized during the day */
		
		public DayState() {}
		
		public DayState(int d, int u, int c) {
			day = d;
			utilizedWindowCapacity = u;
			dailyUsedCapacity = c;
		}
		
		public void setDay(int d) {
			day = d; 
		}
		
		public void SetUtilizedWindowCapacity(int u) {
			utilizedWindowCapacity = u; 
		}

		public void setDailyUsedCapacity(int u) {
			utilizedWindowCapacity = u; 
		}

		public int getDay() {
			return day; 
		}
		
		public int getUtilizedWindowCapacity() {
			return utilizedWindowCapacity; 
		}

		public int getDailyUsedCapacity() {
			return dailyUsedCapacity; 
		}
		

	}
	
	/*
	 * The return structure when called to estimate 
	 * over a comprehensive list of queries
	 */
	public class BundleEstimateResult {
		double conversions;
		double profits;
		
		public BundleEstimateResult() {}
		
		public BundleEstimateResult(double c, double p) {
			conversions = c;
			profits = p;
		}
		
		public void setConversions(double c) {
			conversions = c; 
		}
		
		public void setProfits(double p) {
			profits = p; 
		}

		public double getConversions() {
			return conversions; 
		}
		
		public double getProfits() {
			return profits; 
		}
		
	}
	
	/**
     * Called by optimizer to evaluate the estimated profits for a given bid bundle on specified day 
     * 
     * @param bidBundle a set of query bids and associated limits
     * @param day the day to post bidBundle (not necessarily the current subsequent day!!)
     * @return conversions returned: the total estimated sales (items) 
     * @return profits returned: the total estimated profits
     */
	public BundleEstimateResult estimateBundle(BidBundle bidBundle, DayState day);

	public class QueryEstimateResult {
		double impressions;
		double cpc;
		double conversions;
		double clicks;
		double profits;
		
		public QueryEstimateResult() {}
		
		public QueryEstimateResult(double i, double c, double conv, double clks, double p) {
			impressions = i;
			cpc = c;
			conversions = conv;
			clicks = clks;
			profits = p;
		}

		public void setImpressions(double i) {
			impressions = i; 
		}
		
		public void setCpc(double c) {
			cpc = c; 
		}
		
		
		public void setConversions(double conv) {
			conversions = conv; 
		}

		public void setClicks(double clks) {
			clicks = clks; 
		}
		
		public void setProfits(double p) {
			profits = p; 
		}
		
		public double getImpressions() {
			return impressions; 
		}
		
		public double getCpc() {
			return cpc; 
		}
		
		
		public double getConversions() {
			return conversions; 
		}

		public double getClicks() {
			return clicks; 
		}
		
		public double getProfits() {
			return profits; 
		}
		
	}
	

	/**
     * Called by optimizer to evaluate the estimated figures for a given bid/ad of a query on specified day 
     * 
     * @param query the query
     * @param bid bid on the query
     * @param ad ad associated to the query in the bid
     * @param limit limit associated to the query in the bid
     * @param day the relevant day
     * @return impressions returned: the estimated number of impressions  
     * @return cpc returned: the estimated cost upon bidding  
     * @return conversions returned: the estimated conversions  
     * @return clicks returned: the estimated clicks 
     * @return profits returned: the total estimated profits for the query
     */
	public QueryEstimateResult estimateQuery(Query query, double bid, Ad ad, double limit, DayState day);
	
}
