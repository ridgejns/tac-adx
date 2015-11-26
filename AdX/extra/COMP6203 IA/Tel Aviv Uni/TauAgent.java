package agent;

import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.StartInfo;
import se.sics.tasim.props.SimulationStatus;
import se.sics.isl.transport.Transportable;
import edu.umich.eecs.tac.props.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import se.sics.isl.util.ConfigManager;
import arch.Modeler;
import arch.Optimizer;
import arch.Estimator;
import arch.IAgent;

/**
 * TauAgent class implements a TAC/AA advertiser agent.
 *
 * @author Mariano Schain 
 *  based on the "ExampleAgent" provided by Patrick Jordan 
 * @see <a href="http://aa.tradingagents.org/documentation">TAC AA Documentation</a>
 */
public class TauAgent extends Agent implements IAgent {
	
	/**
	 * Version information for the agent
	 */
	private final static String TAU_AGENT_VERSION = "1.0.0";
	public String 			getVersion() {
		return TAU_AGENT_VERSION;
	}
    
	private static final Logger log = Logger.getLogger("agent");
	
   /**
    * The three functionality components 
    */
	Modeler 	modeler;
	Estimator 	estimator;
	Optimizer 	optimizer;
 
   /**
    * One configuration manager for each component
	*/
	ConfigManager config;
	ConfigManager modelerConfig;
	ConfigManager optimizerConfig;
	ConfigManager estimatorConfig;
	
	String modelerName;
	String estimatorName; 
	String optimizerName; 

	String modelerConfigName;
	String estimatorConfigName; 
	String optimizerConfigName; 
	
	String publisherAddress;
	
   /**
    * The latest bid sent to the server by the agent
	*/
	BidBundle bidBundle;
	
	public BidBundle getBidBundle() {
		return bidBundle;
	}
	
	
/** 
 * We maintain the number of the relevant day:
 *  day: today, yday:yesterday, tday:tomorrow 
 * 
 */
	protected int 		day,yday,tday;

	/**
     * Basic simulation information. {@link StartInfo} contains
     * <ul>
     * <li>simulation ID</li>
     * <li>simulation start time</li>
     * <li>simulation length in simulation days</li>
     * <li>actual seconds per simulation day</li>
     * </ul>
     * An agent should receive the {@link StartInfo} at the beginning of the game or during recovery.
     */
    private StartInfo startInfo;
    public StartInfo getStartInfo() {
    	return startInfo;
    }

    /**
     * Basic auction slot information. {@link SlotInfo} contains
     * <ul>
     * <li>the number of regular slots</li>
     * <li>the number of promoted slots</li>
     * <li>promoted slot bonus</li>
     * </ul>
     * An agent should receive the {@link SlotInfo} at the beginning of the game or during recovery.
     * This information is identical for all auctions over all query classes.
     */
    protected SlotInfo slotInfo;
    public SlotInfo getSlotInfo() {
    	return slotInfo;
    }

    /**
     * The retail catalog. {@link RetailCatalog} contains
     * <ul>
     * <li>the product set</li>
     * <li>the sales profit per product</li>
     * <li>the manufacturer set</li>
     * <li>the component set</li>
     * </ul>
     * An agent should receive the {@link RetailCatalog} at the beginning of the game or during recovery.
     */
    protected RetailCatalog retailCatalog;
    public RetailCatalog getRetailCatalog() {
    	return retailCatalog;
    }

    /**
     * The basic advertiser specific information. {@link AdvertiserInfo} contains
     * <ul>
     * <li>the manufacturer specialty</li>
     * <li>the component specialty</li>
     * <li>the manufacturer bonus</li>
     * <li>the component bonus</li>
     * <li>the distribution capacity discounter</li>
     * <li>the address of the publisher agent</li>
     * <li>the distribution capacity</li>
     * <li>the address of the advertiser agent</li>
     * <li>the distribution window</li>
     * <li>the target effect</li>
     * <li>the focus effects</li>
     * </ul>
     * An agent should receive the {@link AdvertiserInfo} at the beginning of the game or during recovery.
     */
    protected AdvertiserInfo advertiserInfo;
    public AdvertiserInfo getAdvertiserInfo() {
    	return advertiserInfo;
    }
    
    /**
     * The basic publisher information. {@link PublisherInfo} contains
     * <ul>
     * <li>the squashing parameter</li>
     * </ul>
     * An agent should receive the {@link PublisherInfo} at the beginning of the game or during recovery.
     */
    protected PublisherInfo publisherInfo;
    public PublisherInfo getPublisherInfo() {
    	return publisherInfo;
    }

    /**
     * The list contains all of the {@link SalesReport sales report} delivered to the agent.  Each
     * {@link SalesReport sales report} contains the conversions and sales revenue accrued by the agent for each query
     * class during the period.
     */
    protected Queue<SalesReport> salesReports;

    /**
     * The list contains all of the {@link QueryReport query reports} delivered to the agent.  Each
     * {@link QueryReport query report} contains the impressions, clicks, cost, average position, and ad displayed
     * by the agent for each query class during the period as well as the positions and displayed ads of all advertisers
     * during the period for each query class.
     */
    protected Queue<QueryReport> queryReports;

    /**
     * List of all the possible queries made available in the {@link RetailCatalog retail catalog}.
     */
    protected Set<Query> querySet;
    
    public Set<Query> getQuerySet() {
    	return querySet;
    }
    
    public TauAgent() {
    	
    	config = new ConfigManager();
    	config.loadConfiguration("config//arch.conf");
    	
    	modelerName = config.getProperty("tauModeler"); 
    	estimatorName = config.getProperty("tauEstimator"); 
    	optimizerName = config.getProperty("tauOptimizer"); 
    	
    	modelerConfigName = config.getProperty("ModelerConfig"); 
    	estimatorConfigName = config.getProperty("EstimatorConfig"); 
    	optimizerConfigName = config.getProperty("OptimizerConfig"); 

    	modelerConfig = new ConfigManager();
    	modelerConfig.loadConfiguration("config//"+modelerConfigName);

    	estimatorConfig = new ConfigManager();
    	estimatorConfig.loadConfiguration("config//"+estimatorConfigName);

    	optimizerConfig = new ConfigManager();
    	optimizerConfig.loadConfiguration("config//"+optimizerConfigName);

    	
    	salesReports = new LinkedList<SalesReport>();
        queryReports = new LinkedList<QueryReport>();
        querySet = new LinkedHashSet<Query>();
        
		try {
			modeler = (Modeler) Class.forName(modelerName).newInstance();
			estimator = (Estimator) Class.forName(estimatorName).newInstance();
			optimizer = (Optimizer) Class.forName(optimizerName).newInstance();
		} catch (Throwable e) {
			log.log(Level.SEVERE, "could not create an instance of" + modelerName +" or " + estimatorName + " or "+ optimizerName + "exception: " + e);
		}
			
		modeler.setAgent(this);
		optimizer.setAgent(this);
		estimator.setAgent(this);

		modeler.setConfigManager(modelerConfig);
		optimizer.setConfigManager(optimizerConfig);
		estimator.setConfigManager(estimatorConfig);		
		
		optimizer.setEstimator(estimator);
		estimator.setModeler(modeler);
    }

    /**
     * Processes the messages received the by agent from the server.
     *
     * @param message the message
     */
    protected void messageReceived(Message message) {
        Transportable content = message.getContent();

        if (content instanceof QueryReport) {
            handleQueryReport((QueryReport) content);
        } else if (content instanceof SalesReport) {
            handleSalesReport((SalesReport) content);
        } else if (content instanceof SimulationStatus) {
            handleSimulationStatus((SimulationStatus) content);
        } else if (content instanceof PublisherInfo) {
            handlePublisherInfo((PublisherInfo) content);
        } else if (content instanceof SlotInfo) {
            handleSlotInfo((SlotInfo) content);
        } else if (content instanceof RetailCatalog) {
            handleRetailCatalog((RetailCatalog) content);
        } else if (content instanceof AdvertiserInfo) {
            handleAdvertiserInfo((AdvertiserInfo) content);
        } else if (content instanceof StartInfo) {
            handleStartInfo((StartInfo) content);
        }
    }

    /**
     * Sends a constructed {@link BidBundle} as indicated by the Optimizer
     */
    protected void sendBidAndAds(int tday) {
     	
        bidBundle = optimizer.optimize();
        if (publisherAddress != null) {
            sendMessage(publisherAddress, bidBundle);
        }
    }


    /**
     * Processes an incoming query report.
     *
     * @param queryReport the daily query report received from the game server.
     */
    protected void handleQueryReport(QueryReport queryReport) {
        if (yday > 0) {
        	queryReports.add(queryReport);
        	modeler.handleQueryReport(queryReport,yday);
    		estimator.handleQueryReport(queryReport,yday);
    		optimizer.handleQueryReport(queryReport,yday);
        }
    }

    /**
     * Processes an incoming sales report.
     *
     * @param salesReport the daily sales report.
     */
    protected void handleSalesReport(SalesReport salesReport) {
        if (yday > 0) {
        	salesReports.add(salesReport);
           	modeler.handleSalesReport(salesReport,yday);
    		estimator.handleSalesReport(salesReport,yday);
    		optimizer.handleSalesReport(salesReport,yday);        	
        }
   }

    /**
     * Processes a simulation status notification.  Each simulation day the {@link SimulationStatus simulation status }
     * notification is sent after the other daily messages ({@link QueryReport} {@link SalesReport} have been sent.
     *
     * @param simulationStatus the daily simulation status.
     */
    protected void handleSimulationStatus(SimulationStatus simulationStatus) {
        sendBidAndAds(tday);
        nextDay();
    }

    protected void nextDay() {
        log.log(Level.FINE, " day "+day +" ending");
    	yday = day;
        day = tday;
        tday = tday + 1;
		modeler.nextDay(day);
		optimizer.nextDay(day);
		estimator.nextDay(day);
        log.log(Level.FINE, " starting day "+day);
    }
    
    /**
     * Processes the publisher information.
     * @param publisherInfo the publisher information.
     */
    protected void handlePublisherInfo(PublisherInfo publisherInfo) {
        this.publisherInfo = publisherInfo;
    }

    /**
     * Processrs the slot information.
     * @param slotInfo the slot information.
     */
    protected void handleSlotInfo(SlotInfo slotInfo) {
        this.slotInfo = slotInfo;
    }

    /**
     * Processes the retail catalog.
     * @param retailCatalog the retail catalog.
     */
    protected void handleRetailCatalog(RetailCatalog retailCatalog) {
        this.retailCatalog = retailCatalog;

        // The query space is all the F0, F1, and F2 queries for each product
        // The F0 query class
        if(retailCatalog.size() > 0) {
            querySet.add(new Query(null, null));
        }

        for(Product product : retailCatalog) {
            // The F1 query classes
            // F1 Manufacturer only
            querySet.add(new Query(product.getManufacturer(), null));
            // F1 Component only
            querySet.add(new Query(null, product.getComponent()));

            // The F2 query class
            querySet.add(new Query(product.getManufacturer(), product.getComponent()));
        }
    }

    /**
     * Processes the advertiser information.
     * @param advertiserInfo the advertiser information.
     */
    protected void handleAdvertiserInfo(AdvertiserInfo advertiserInfo) {
        this.advertiserInfo = advertiserInfo;
 
        publisherAddress = advertiserInfo.getPublisherId();
        
		modeler.simulationReady();
		estimator.simulationReady();
		optimizer.simulationReady();		
    }

    /**
     * Processes the start information.
     * @param startInfo the start information.
     */
    protected void handleStartInfo(StartInfo startInfo) {
        this.startInfo = startInfo;
    }

    public String getAgentName() {
    	return config.getProperty("agentName");
    }
    
    /**
     * Prepares the agent for a new simulation.
     */
    protected void simulationSetup() {
		day = 0; tday= 1; yday = -1;
    	modeler.simulationSetup();
		estimator.simulationSetup();
		optimizer.simulationSetup();
    }

    /**
     * Runs any post-processes required for the agent after a simulation ends.
     */
    protected void simulationFinished() {
        
		modeler.simulationFinished();
		estimator.simulationFinished();
		optimizer.simulationFinished();

    	salesReports.clear();
        queryReports.clear();
        querySet.clear();
    }
}
