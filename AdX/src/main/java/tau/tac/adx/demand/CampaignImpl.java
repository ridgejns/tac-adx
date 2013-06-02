package tau.tac.adx.demand;

import static edu.umich.eecs.tac.auction.AuctionUtils.hardSort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import tau.tac.adx.AdxManager;
import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.devices.Device;
import tau.tac.adx.messages.CampaignLimitSet;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.users.AdxUser;

import com.google.common.eventbus.Subscribe;

/**
 * 
 * @author Mariano Schain
 * 
 */
public class CampaignImpl implements Campaign, Accumulator<CampaignStats> {
	private final Logger log = Logger.getLogger(CampaignImpl.class.getName());
	private final static double ERRA = 4.08577;
	private final static double ERRB = -3.08577;

	private final static Long DEFAULT_BUDGET_FACTOR = 1L;

	/* maintains quality score - notified upon campaign end */
	protected QualityManager qualityManager;

	int id;
	/* contract attributes */
	Long reachImps;
	int dayStart;
	int dayEnd;
	Set<MarketSegment> targetSegments;
	double videoCoef;
	double mobileCoef;

	/* auction info */
	private final static double RESERVE_BUDGET_FACTOR = 1.0;
	private final Map<String, Long> advertisersBids;

	/* set upon campaign allocation/auction */
	Double budget;
	String advertiser;

	/* current day id and accounting info */
	protected int day;
	private CampaignStats todays;
	private CampaignStats totals;

	private Double budgetlimit;
	private int impressionLimit;

	/**
	 * @return the log
	 */
	public Logger getLog() {
		return log;
	}

	/**
	 * @return the erra
	 */
	public static double getErra() {
		return ERRA;
	}

	/**
	 * @return the errb
	 */
	public static double getErrb() {
		return ERRB;
	}

	/**
	 * @return the defaultBudgetFactor
	 */
	public static Long getDefaultBudgetFactor() {
		return DEFAULT_BUDGET_FACTOR;
	}

	/**
	 * @return the qualityManager
	 */
	public QualityManager getQualityManager() {
		return qualityManager;
	}

	/**
	 * @return the reserveBudgetFactor
	 */
	public static double getReserveBudgetFactor() {
		return RESERVE_BUDGET_FACTOR;
	}

	/**
	 * @return the advertisersBids
	 */
	public Map<String, Long> getAdvertisersBids() {
		return advertisersBids;
	}

	/**
	 * @return the day
	 */
	public int getDay() {
		return day;
	}

	/**
	 * @return the todays
	 */
	public CampaignStats getTodays() {
		return todays;
	}

	/**
	 * @return the budgetlimit
	 */
	public Double getBudgetlimit() {
		return budgetlimit;
	}

	/**
	 * @return the impressionLimit
	 */
	@Override
	public int getImpressionLimit() {
		return impressionLimit;
	}

	/**
	 * @return the tomorrowsBudgetLimit
	 */
	public Double getTomorrowsBudgetLimit() {
		return tomorrowsBudgetLimit;
	}

	/**
	 * @return the tomorrowsImpressionLimit
	 */
	public int getTomorrowsImpressionLimit() {
		return tomorrowsImpressionLimit;
	}

	/**
	 * @return the dayStats
	 */
	public SortedMap<Integer, CampaignStats> getDayStats() {
		return dayStats;
	}

	private Double tomorrowsBudgetLimit;
	private int tomorrowsImpressionLimit;

	private final SortedMap<Integer, CampaignStats> dayStats;

	public CampaignImpl(QualityManager qualityManager, int reachImps,
			int dayStart, int dayEnd, Set<MarketSegment> targetSegments,
			double videoCoef, double mobileCoef) {

		if (qualityManager == null)
			throw new NullPointerException("qualityManager cannot be null");

		id = hashCode();
		dayStats = new TreeMap<Integer, CampaignStats>();
		advertisersBids = new HashMap<String, Long>();
		budget = null;
		advertiser = null;
		budgetlimit = Double.POSITIVE_INFINITY;
		tomorrowsBudgetLimit = Double.POSITIVE_INFINITY;
		impressionLimit = Integer.MAX_VALUE;
		tomorrowsImpressionLimit = Integer.MAX_VALUE;
		/* the first day for the campaign to be collecting statistics */
		day = dayStart;

		this.qualityManager = qualityManager;
		this.reachImps = (long) reachImps;
		this.dayStart = dayStart;
		this.dayEnd = dayEnd;
		this.targetSegments = targetSegments;
		this.videoCoef = videoCoef;
		this.mobileCoef = mobileCoef;

		todays = new CampaignStats(0.0, 0.0, 0.0);
		totals = new CampaignStats(0.0, 0.0, 0.0);
	}

	@Override
	public void registerToEventBus() {
		AdxManager.getInstance().getSimulation().getEventBus().register(this);
	}

	@Override
	public Double getBudget() {
		return budget;
	}

	@Override
	public Long getReachImps() {
		return reachImps;
	}

	@Override
	public int getDayStart() {
		return dayStart;
	}

	@Override
	public int getDayEnd() {
		return dayEnd;

	}

	public void setTomorowsLimit(CampaignLimitSet message) {
		this.tomorrowsBudgetLimit = message.getBudgetLimit();
		this.tomorrowsImpressionLimit = message.getImpressionLimit();
		
		log.log(Level.INFO, "Campaign " + id + " Tomorrows limits: " 
		   + tomorrowsBudgetLimit + ", "+ tomorrowsImpressionLimit);
	}

	@Override
	public boolean isOverTodaysLimit() {
		return (budgetlimit < /*totals.cost + */todays.cost)
				|| (impressionLimit < /*totals.tartgetedImps
						+ */todays.tartgetedImps);
	}

	@Override
	public Set<MarketSegment> getTargetSegment() {
		return targetSegments;

	}

	@Override
	public double getVideoCoef() {
		return videoCoef;

	}

	@Override
	public double getMobileCoef() {
		return mobileCoef;

	}

	@Override
	public void impress(AdxUser adxUser, AdType adType, Device device,
			double costPerMille) {
		if (isAllocated()) {
			todays.cost += costPerMille / 1000.0;
			if (todays.cost > budgetlimit) {
				int i = 0;
			}

			double imps = (device == Device.mobile ? mobileCoef : 1.0)
					* (adType == AdType.video ? videoCoef : 1.0);

			if (MarketSegment.extractSegment(adxUser).contains(targetSegments)) {
				todays.tartgetedImps += imps;
			} else {
				todays.otherImps += imps;
			}
		}
	}

	double effectiveReachRatio(double imps) {
		double ratio = imps / reachImps;
		return (2.0 / ERRA)
				* (Math.atan(ERRA * ratio + ERRB) - Math.atan(ERRB));
	}

	@Override
	public void preNextTimeUnit(int timeUnit) {
		/*
		if (todays.tartgetedImps > impressionLimit + 10
				&& impressionLimit != (int) Double.POSITIVE_INFINITY) {
			String s = "\nbudgetlimit: " + budgetlimit + " totals.cost: "
					+ totals.cost + " todays.cost: " + todays.cost
					+ " impressionLimit: " + impressionLimit
					+ " totals.tartgetedImps: " + totals.tartgetedImps
					+ " todays.tartgetedImps: " + todays.tartgetedImps + "\n";
			throw new RuntimeException(" campaign id: " + this.id + " "
					+ todays.toString() + " impresssion limit: "
					+ this.impressionLimit + s);
		}
        */
		
		dayStats.put(day, todays);
		totals = totals.add(todays);
		todays = new CampaignStats(0.0, 0.0, 0.0);
		day = timeUnit;
		
		budgetlimit = tomorrowsBudgetLimit;
		tomorrowsBudgetLimit = Double.POSITIVE_INFINITY;
		impressionLimit = tomorrowsImpressionLimit;		
		tomorrowsImpressionLimit = Integer.MAX_VALUE;
		
		if (day == dayEnd + 1) { /* was last day - update quality score */
			double effectiveReachRatio = effectiveReachRatio(totals.tartgetedImps);
			qualityManager.updateQualityScore(advertiser, effectiveReachRatio);
			AdxManager
					.getInstance()
					.getSimulation()
					.broadcastAdNetworkRevenue(advertiser,
							effectiveReachRatio * budget);

			log.log(Level.INFO, "Campaign " + id + " ended for advertiser "
					+ advertiser + ". Stats " + totals + " Reach " + reachImps
					+ " ERR " + effectiveReachRatio + " Budget " + budget
					+ " Revenue " + effectiveReachRatio * budget);
		}
	}

	@Override
	public String getAdvertiser() {
		return advertiser;
	}

	@Override
	public void addAdvertiserBid(String advertiser, Long budgetBid) {
		/* bids above the reserve budget are not considered */
		if ((budgetBid > 0)
				&& (budgetBid <= (RESERVE_BUDGET_FACTOR * reachImps)))
			advertisersBids.put(advertiser, budgetBid);
	}

	@Override
	public Map<String, Long> getBiddingAdvertisers() {
		return advertisersBids;
	}

	@Override
	public void allocateToAdvertiser(String advertiser) {
		budget = new Double(reachImps * DEFAULT_BUDGET_FACTOR) / 1000.0;
		this.advertiser = advertiser;
	}

	@Override
	public void auction() {
		double bsecond;
		int advCount = advertisersBids.size();
		advertiser = "";
		if (advCount > 0) {
			String[] advNames = new String[advCount];
			double[] qualityScores = new double[advCount];
			double[] bids = new double[advCount];
			double[] scores = new double[advCount];
			int[] indices = new int[advCount];

			int i = 0;
			for (String advName : advertisersBids.keySet()) {
				advNames[i] = new String(advName);
				bids[i] = advertisersBids.get(advName);
				qualityScores[i] = qualityManager.getQualityScore(advName);
				scores[i] = qualityScores[i] / bids[i];
				indices[i] = i;
				i++;
			}

			hardSort(scores, indices);

			advertiser = advNames[indices[0]];

			double reserveScore = 1.0 / (RESERVE_BUDGET_FACTOR * reachImps);

			if (advCount == 1)
				bsecond = reserveScore;
			else
				bsecond = (scores[indices[1]] > reserveScore) ? scores[indices[1]]
						: reserveScore;

			budget = new Double(qualityScores[indices[0]] / (1000.0 * bsecond));
		}
	}

	@Override
	public int getRemainingDays() {
		if (day > dayEnd) {
			return 0;
		} else {
			int cday = (day <= dayStart) ? dayStart : day;
			return dayEnd - cday + 1;
		}
	}

	@Override
	public boolean isActive() {
		return (isAllocated() && (day <= dayEnd) && (day >= dayStart));
	}

	@Override
	public boolean isAllocated() {
		return (budget != null) && (advertiser != null);
	}

	@Override
	public CampaignStats getStats(int timeUnitFrom, int timeUnitTo) {
		CampaignStats current = (timeUnitTo >= day) ? todays : null; /*
																	 * should
																	 * add
																	 * current
																	 * stats to
																	 * accumulated
																	 */
		SortedMap<Integer, CampaignStats> daysRangeStats = dayStats.subMap(
				timeUnitFrom, timeUnitTo + 1);

		return AccumulatorImpl.accumulate(this,
				new ArrayList<CampaignStats>(daysRangeStats.values()),
				new CampaignStats(0.0, 0.0, 0.0)).add(current);
	}

	@Subscribe
	public void limitSet(CampaignLimitSet message) {
		if ((message.getCampaignId() == id)
				&& (message.getAdNetwork().equals(advertiser))) {
			setTomorowsLimit(message);
		}
	}

	@Override
	public CampaignStats getTodayStats() {
		return todays;
	}

	@Override
	public CampaignStats accumulate(CampaignStats interim, CampaignStats next) {
		return interim.add(next);
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String toString() {
		return "CampaignImpl [qualityManager=" + qualityManager + ", id=" + id
				+ ", reachImps=" + reachImps + ", dayStart=" + dayStart
				+ ", dayEnd=" + dayEnd + ", targetSegment=" + targetSegments
				+ ", videoCoef=" + videoCoef + ", mobileCoef=" + mobileCoef
				+ ", advertisersBids=" + advertisersBids + ", budget=" + budget
				+ ", advertiser=" + advertiser + ", day=" + day + ", todays="
				+ todays + ", totals=" + totals + ", dayStats=" + dayStats
				+ "]";
	}

	@Override
	public void nextTimeUnit(int timeUnit) {
		// TODO Auto-generated method stub

	}

	@Override
	public CampaignStats getTotals() {
		if (totals.getTargetedImps() > 1300) {
			int i = 0;
		}
		return totals;
	}

}
