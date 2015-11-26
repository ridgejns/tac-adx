/**
 * 
 */
package arch;

import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.Query;

/**
 * This interface should be implemented by any Modeler
 *
 * @author Mariano Schain
 */
public interface IModeler {
	public class ModelerResult {
		double cpc;
		double impressions;
		double position;
		
		public ModelerResult(double c, double i, double p) {
			cpc = c;
			impressions = i;
			position = p;
		}
		
		public void setCpc(double c) {
			cpc = c;
		}
		
		public void setImpressions(double i) {
			impressions = i;
		}

		public void setPosition(double p) {
			position = p;
		}
		
		public double getCpc() {
			return cpc;
		}
		
		public double getImpressions() {
			return impressions;
		}

		public double getPosition() {
			return position;
		}
	}
	
	
	/**
     * Called by estimator to evaluate the impressions and cost for a given bid and ad for a query on a specified day  
     * 
     * @param query the relevant query
     * @param bid the bid amount
     * @param ad the associated ad 
     * @param day the relevant day 
     * @return impressions returned: the modeled impressions 
     * @return cost returned: the modeled cpc
     * @return position returned: the modeled average position
     */

	public ModelerResult model(Query query, double bid, Ad ad, int day);
}
