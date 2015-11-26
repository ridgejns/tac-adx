package arch;

import java.util.Set;

import se.sics.tasim.props.StartInfo;
import edu.umich.eecs.tac.props.*;

/**
 * This interface is implemented by Tau Agent and may be 
 * used by the functionality components to access status information
 *
 * @author Mariano Schain
 */

public interface IAgent {
	public Set<Query>		getQuerySet();	
	
	/* 
	 * returns the latest message received from the server
	 */	
	public StartInfo 		getStartInfo();
	public SlotInfo 		getSlotInfo();
	public RetailCatalog 	getRetailCatalog();
	public AdvertiserInfo 	getAdvertiserInfo();
	public PublisherInfo 	getPublisherInfo();

	/* 
	 * returns the latest BidBundle sent to the server
	 */
	public BidBundle		getBidBundle();
	
	public String 			getAgentName();
	
	public String 			getVersion();
}
