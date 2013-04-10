package tau.tac.adx.agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import se.sics.isl.transport.Transportable;
import se.sics.tasim.aw.Agent;
import se.sics.tasim.aw.Message;
import se.sics.tasim.props.SimulationStatus;
import se.sics.tasim.props.StartInfo;
import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.demand.CampaignStats;
import tau.tac.adx.devices.Device;
import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.props.PublisherCatalogEntry;
import tau.tac.adx.report.adn.AdNetworkKey;
import tau.tac.adx.report.adn.AdNetworkReport;
import tau.tac.adx.report.adn.AdNetworkReportEntry;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.report.demand.AdNetBidMessage;
import tau.tac.adx.report.demand.AdNetworkDailyNotification;
import tau.tac.adx.report.demand.CampaignOpportunityMessage;
import tau.tac.adx.report.demand.CampaignReport;
import tau.tac.adx.report.demand.CampaignReportKey;
import tau.tac.adx.report.demand.InitialCampaignMessage;
import tau.tac.adx.report.publisher.AdxPublisherReport;
import tau.tac.adx.report.publisher.AdxPublisherReportEntry;
import edu.umich.eecs.tac.props.Ad;
import edu.umich.eecs.tac.props.BankStatus;

public class SampleAdNetwork extends Agent {

	private final Logger log = Logger
			.getLogger(SampleAdNetwork.class.getName());

	/*
	 * Basic simulation information. An agent should receive the {@link
	 * StartInfo} at the beginning of the game or during recovery.
	 */
	@SuppressWarnings("unused")
	private StartInfo startInfo;

	/**
	 * Messages received:
	 * 
	 * We keep all the {@link CampaignReport campaign reports} and
	 * {@link AdxPublisherReport publisher reports} delivered to the agent. We
	 * also keep the initialization messages {@link PublisherCatalog} and
	 * {@link InitialCampaignMessage} and the most recent messages and reports
	 * {@link CampaignOpportunityMessage}, {@link CampaignReport}, and
	 * {@link AdNetworkDailyNotification}.
	 */
	private Queue<CampaignReport> campaignReports;
	private Queue<AdxPublisherReport> adxPublisherReports;
	private PublisherCatalog publisherCatalog;
	private InitialCampaignMessage initialCampaignMessage;
	private AdNetworkDailyNotification adNetworkDailyNotification;

	/*
	 * The addresses of server entities to which the agent should send the daily
	 * bids data
	 */
	private String demandAgentAddress;
	private String adxAgentAddress;

	/*
	 * we maintain a list of queries - each characterized by the web site (the
	 * publisher), the device type, the ad type, and the user market segment
	 */
	private AdxQuery[] queries;

	/**
	 * Information regarding the latest campaign opportunity announced
	 */
	private CampaignData pendingCampaign;

	/**
	 * We maintain a collection (mapped by the campaign id) of the campaigns won
	 * by our agent.
	 */
	private Map<Integer, CampaignData> myCampaigns;

	/*
	 * the bidBundle to be sent daily to the AdX
	 */
	private AdxBidBundle bidBundle;

	/*
	 * The current bid level for the user classification service
	 */
	double ucsBid;

	/*
	 * The targeted service level for the user classification service
	 */
	double ucsTargetLevel;

	/*
	 * current day of simulation
	 */
	private int day;

	private Random randomGenerator;

	public SampleAdNetwork() {
		campaignReports = new LinkedList<CampaignReport>();
	}

	@Override
	protected void messageReceived(Message message) {
		try {
			Transportable content = message.getContent();
			log.fine(message.getContent().getClass().toString());
			if (content instanceof InitialCampaignMessage) {
				handleInitialCampaignMessage((InitialCampaignMessage) content);
			} else if (content instanceof CampaignOpportunityMessage) {
				handleICampaignOpportunityMessage((CampaignOpportunityMessage) content);
			} else if (content instanceof CampaignReport) {
				handleCampaignReport((CampaignReport) content);
			} else if (content instanceof AdNetworkDailyNotification) {
				handleAdNetworkDailyNotification((AdNetworkDailyNotification) content);
			} else if (content instanceof AdxPublisherReport) {
				handleAdxPublisherReport((AdxPublisherReport) content);
			} else if (content instanceof SimulationStatus) {
				handleSimulationStatus((SimulationStatus) content);
			} else if (content instanceof PublisherCatalog) {
				handlePublisherCatalog((PublisherCatalog) content);
			} else if (content instanceof AdNetworkReport) {
				handleAdNetworkReport((AdNetworkReport) content);
			} else if (content instanceof StartInfo) {
				handleStartInfo((StartInfo) content);
			} else if (content instanceof BankStatus) {
				handleBankStatus((BankStatus) content);
			} else {
				log.info("UNKNOWN Message Received: "+content);
			}

		} catch (NullPointerException e) {
			this.log.log(Level.SEVERE,
					"Exception thrown while trying to parse message." + e);
			return;
		}
	}

	private void handleBankStatus(BankStatus content) {
		log.info(content.toString());		
	}

	/**
	 * Processes the start information.
	 * 
	 * @param startInfo
	 *            the start information.
	 */
	protected void handleStartInfo(StartInfo startInfo) {
		this.startInfo = startInfo;
	}

	/**
	 * Process the reported set of publishers
	 * 
	 * @param publisherCatalog
	 */
	private void handlePublisherCatalog(PublisherCatalog publisherCatalog) {
		this.publisherCatalog = publisherCatalog;
		generateAdxQuerySpace();
	}

	/**
	 * On day 0, a campaign (the "initial campaign") is allocated to each
	 * competing agent. The campaign starts on day 1. The address of the
	 * server's AdxAgent (to which bid bundles are sent) and DemandAgent (to
	 * which bids regarding campaign opportunities may be sent in subsequent
	 * days) are also reported in the initial campaign message
	 */
	private void handleInitialCampaignMessage(
			InitialCampaignMessage campaignMessage) {
		log.info(campaignMessage.toString());

		day = 0;

		initialCampaignMessage = campaignMessage;
		demandAgentAddress = campaignMessage.getDemandAgentAddress();
		adxAgentAddress = campaignMessage.getAdxAgentAddress();

		CampaignData campaignData = new CampaignData(initialCampaignMessage);

		/*
		 * The initial campaign is already allocated to our agent so we add it
		 * to our allocated-campaigns list.
		 */
		log.info("Day " + day + ": Allocated campaign - " + campaignData);
		myCampaigns.put(initialCampaignMessage.getId(), campaignData);
	}

	/**
	 * On day n ( > 0) a campaign opportunity is announced to the competing
	 * agents. The campaign starts on day n + 2 or later and the agents may send
	 * (on day n) related bids (attempting to win the campaign). The allocation
	 * (the winner) is announced to the competing agents during day n + 1.
	 */
	private void handleICampaignOpportunityMessage(
			CampaignOpportunityMessage com) {

		day = com.getDay();

		pendingCampaign = new CampaignData(com);
		log.info("Day " + day + ": Campaign opportunity - " + pendingCampaign);

		/*
		 * The campaign requires com.getReachImps() impressions. The competing
		 * Ad Networks bid for the total campaign Budget (that is, the ad
		 * network that offers the lowest budget gets the campaign allocated).
		 * The advertiser is willing to pay the AdNetwork at most 1$ CPM,
		 * therefore the total number of impressions may be treated as a reserve
		 * (upper bound) price for the auction. 
		 */
		long cmpBid = 1 + Math.abs((randomGenerator.nextLong()) % (com.getReachImps()));
		
		double cmpBidUnits = cmpBid/1000.0;

		log.info("Day " + day +": Campaign total budget bid: " + cmpBidUnits);

		/*
		 * Adjust ucs bid s.t. target level is achieved.
		 */
		
		if (adNetworkDailyNotification != null) {
			double ucsLevel = adNetworkDailyNotification.getServiceLevel();
			double prevUcsBid = ucsBid;

			ucsBid = prevUcsBid	* (1 + ucsTargetLevel - ucsLevel);

			log.info("Day " + day + ": Adjusting ucs bid: was " + prevUcsBid + " level reported: " + ucsLevel
					+ " target: " + ucsTargetLevel + " adjusted: " + ucsBid);
		} else {
			log.info("Day " + day + ": Initial ucs bid is " + ucsBid);			
		}

		
		/*
		 * the bid for the user classification service is piggybacked
		 */
		AdNetBidMessage bids = new AdNetBidMessage(ucsBid, pendingCampaign.id, cmpBid);
		sendMessage(demandAgentAddress, bids);
	}

	/**
	 * On day n ( > 0), the result of the UserClassificationService and Campaign
	 * auctions (for which the competing agents sent bids during day n -1) are
	 * reported. The reported Campaign starts in day n+1 or later and the user
	 * classification service level is applicable starting from day n+1.
	 */
	private void handleAdNetworkDailyNotification(
			AdNetworkDailyNotification notificationMessage) {

		if (day ==0) ++day; /* this is the first message sent by the server on day 1 */
		
		adNetworkDailyNotification = notificationMessage;

		log.info("Day " + day + ": Daily notification for campaign " + 
				adNetworkDailyNotification.getCampaignId());

		String campaignAllocatedTo = " allocated to " + notificationMessage.getWinner();

		/*
		if ((pendingCampaign.id == adNetworkDailyNotification.getCampaignId())
				&& getName().equals(notificationMessage.getWinner())) {
        */
		if ((pendingCampaign.id == adNetworkDailyNotification.getCampaignId())
				&& (notificationMessage.getCost() != 0) ) {

		/* add campaign to list of won campaigns */
			myCampaigns.put(pendingCampaign.id, pendingCampaign);
			
			campaignAllocatedTo = " WON at cost " + notificationMessage.getCost();
		}

		log.info("Day " + day + ": " + campaignAllocatedTo + ". UCS Level set to "
				+ notificationMessage.getServiceLevel() + " at price "
				+ notificationMessage.getPrice()
				);
	}

	/**
	 * The SimulationStatus message received on day n indicates that the
	 * calculation time is up and the agent is requested to send its bid bundle
	 * to the AdX.
	 */
	private void handleSimulationStatus(SimulationStatus simulationStatus) {
		sendBidAndAds();
	}

	/**
	 * 
	 */
	protected void sendBidAndAds() {

		bidBundle = new AdxBidBundle();

		/*
		 * 
		 */
		for (CampaignData campaign : myCampaigns.values()) {
			
			int dayBiddingFor = day + 1;
			
			/* a random bid, for all queries       */
			/* bidding (CPM) randomly in (0.001,1) */
			Random rnd = new Random();
			long nextLong = rnd.nextLong();
			double rbid = ((1 + nextLong) % 1000)/1000.0;
						
			/* 
			 * add bid entries w.r.t. each active campaign with 
			 * remaining contracted impressions.
			 * 
			 * for now, a single entry per active campaign is added 
   		     * for queries of matching target segment. 
			 */ 
			
			if ((dayBiddingFor >= campaign.dayStart)
					&& (dayBiddingFor <= campaign.dayEnd)
					&& (campaign.impsTogo() >= 0)
					) {


				int entCount = 0;
				for (int i = 0; i < queries.length; i++) {
					
					Set<MarketSegment> segmentsList = queries[i].getMarketSegments();
					
					for (MarketSegment marketSegment : segmentsList) {
						if (campaign.targetSegment == marketSegment) {
							/* 
							 * among matching entries with the same campaign id,    
							 * the AdX randomly chooses an entry according to the       
							 * designated weight.                                           
							 * by setting a constant weight 1, 
							 * we create a uniform probability over active campaigns 
							 */
							++entCount;
							bidBundle.addQuery(queries[i], rbid, new Ad(null), campaign.id, 1);						
						}
					}
					if (segmentsList.size() == 0) {
						++entCount;
						bidBundle.addQuery(queries[i], rbid, new Ad(null), campaign.id, 1);
					}
				}
				bidBundle.setCampaignDailySpendLimit(campaign.id, campaign.impsTogo()/1000.0);
				
				log.info("Day " + day + ": Updated "+ entCount + " Bid Bundle entries for Campaign id " + campaign.id);				
			}
		}

		if (bidBundle != null) {
			sendMessage(adxAgentAddress, bidBundle);
		}
	}

	/**
	 * Campaigns performance w.r.t. each allocated campaign
	 */
	private void handleCampaignReport(CampaignReport campaignReport) {

		if (day >=1 ) ++day; /* this is the first message sent by the server on day >= 2 */

		campaignReports.add(campaignReport);
		
		/* for each campaign, the accumulated statistics from day 1 up to day n-1 are reported */
		for (CampaignReportKey campaignKey : campaignReport.keys()) {
			int cmpId = campaignKey.getCampaignId();
			CampaignStats cstats = campaignReport.getCampaignReportEntry(campaignKey).getCampaignStats();
			myCampaigns.get(cmpId).setStats(cstats);
			
			log.info("Day " + day + ": Updating campaign " + cmpId +" stats: " + 
					cstats.getTargetedImps() + " tgtImps " + 
					cstats.getOtherImps() + " nonTgtImps. Cost of imps is " + 
					cstats.getCost()
					);
		}		
	}

	/**
	 * Users and Publishers statistics: popularity and ad type orientation
	 */
	private void handleAdxPublisherReport(AdxPublisherReport adxPublisherReport) {
		adxPublisherReports.add(adxPublisherReport);
		
		log.info("Publishers Report: ");		
		for (PublisherCatalogEntry publisherKey : adxPublisherReport.keys()) {
			AdxPublisherReportEntry entry = adxPublisherReport.getEntry(publisherKey);
			log.info(entry.toString());
	    }
	}

	/**
	 * 
	 * @param AdNetworkReport
	 */
	private void handleAdNetworkReport(AdNetworkReport adnetReport) {
		log.info("AdNetreport: ");		
		for (AdNetworkKey adnetKey : adnetReport.keys()) {
			double rnd = Math.random();
			if (rnd>0.95) {
				AdNetworkReportEntry entry = adnetReport.getAdNetworkReportEntry(adnetKey);
				log.info(adnetKey + " " + entry);
			}
		}
		
	}

	@Override
	protected void simulationSetup() {
		randomGenerator = new Random();
		day = 0;
		bidBundle = new AdxBidBundle();
		ucsTargetLevel = 0.5 + (randomGenerator.nextInt(5) + 1) / 10.0;
		ucsBid = randomGenerator.nextInt(10);
		myCampaigns = new HashMap<Integer, CampaignData>();
		log.fine("AdNet " + getName() + " simulationSetup");
	}

	@Override
	protected void simulationFinished() {
		campaignReports.clear();
		adxPublisherReports.clear();
		bidBundle = null;
	}

	/**
	 * A user visit to a publisher's web-site results in an impression
	 * opportunity (a query) that is characterized by the the publisher, the
	 * market segment the user may belongs to, the device used (mobile or
	 * desktop) and the ad type (text or video).
	 * 
	 * An array of all possible queries is generated here, based on the
	 * publisher names reported at game initialization in the publishers catalog
	 * message
	 */
	private void generateAdxQuerySpace() {
		if (publisherCatalog != null && queries == null) {
			Set<AdxQuery> querySet = new HashSet<AdxQuery>();

			/*
			 * for each web site (publisher) we generate all possible variations
			 * of device type, ad type, and user market segment
			 */
			for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog) {
				String publishersName = publisherCatalogEntry
						.getPublisherName();
				for (MarketSegment userSegment : MarketSegment.values()) {
					Set<MarketSegment> singleMarketSegment = new HashSet<MarketSegment>();
					singleMarketSegment.add(userSegment);

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.video));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.video));

				}
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile, AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile, AdType.text));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.text));
			}
			queries = new AdxQuery[querySet.size()];
			querySet.toArray(queries);
		}
	}

	private class CampaignData {
		public CampaignData(InitialCampaignMessage icm) {
			reachImps = icm.getReachImps();
			dayStart = icm.getDayStart();
			dayEnd = icm.getDayEnd();
			targetSegment = icm.getTargetSegment();
			videoCoef = icm.getVideoCoef();
			mobileCoef = icm.getMobileCoef();
			id = icm.getId();
			stats = new CampaignStats(0,0,0);
		}

		public CampaignData(CampaignOpportunityMessage com) {
			dayStart = com.getDayStart();
			dayEnd = com.getDayEnd();
			id = com.getId();
			reachImps = com.getReachImps();
			targetSegment = com.getTargetSegment();
			mobileCoef = com.getMobileCoef();
			videoCoef = com.getVideoCoef();
			stats = new CampaignStats(0,0,0);
		}

		@Override
		public String toString() {
			return "Campaign ID " + id + ": " + "day " + dayStart + " to "
					+ dayEnd + " " + targetSegment.name() + ", reach: "
					+ reachImps + " coefs: (v=" + videoCoef + ", m="
					+ mobileCoef + ")";
		}

		Long impsTogo() {
			return reachImps - (long)stats.getTargetedImps();
		}
		
		void setStats(CampaignStats s) {
		   stats.setValues(s);	
		}
		
		/* campaign attributes as set by server */
		Long reachImps;
		long dayStart;
		long dayEnd;
		MarketSegment targetSegment;
		double videoCoef;
		double mobileCoef;
		int id;
		
		/* campaign info as reported */
		CampaignStats stats;		
	}

}
