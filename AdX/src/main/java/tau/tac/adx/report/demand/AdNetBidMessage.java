package tau.tac.adx.report.demand;

import java.text.ParseException;

import se.sics.isl.transport.TransportReader;
import se.sics.isl.transport.TransportWriter;
import se.sics.tasim.props.SimpleContent;

public class AdNetBidMessage extends SimpleContent {

	private static final long serialVersionUID = -4773426215378916401L;

	private double ucsBid;
	private int campaignId;
	private Long campaignBudgetBid;
	
	public AdNetBidMessage(double ucsBid, int campaignId, Long campaignBudgetBid) {
		this.ucsBid = ucsBid;
		this.campaignId = campaignId;
		this.campaignBudgetBid = campaignBudgetBid;
	}
	
	public AdNetBidMessage(int id, Long budget) {				
		this.campaignId = id;
		this.campaignBudgetBid = budget;
	}

	public double getUcsBid() {
		return ucsBid;
	}

	public Long getCampaignBudget() {
		return campaignBudgetBid;
	}

	public int getCampaignId() {
		return campaignId;
	}

	
	public String toString() {
		StringBuffer buf = new StringBuffer().append(getTransportName())
				.append('[').append(ucsBid).append(',').append(campaignId).append(',').append(campaignBudgetBid).append(',');
		return params(buf).append(']').toString();
	}

	
	public void read(TransportReader reader) throws ParseException {
		if (isLocked()) {
			throw new IllegalStateException("locked");
		}
		ucsBid = reader.getAttributeAsDouble("ucs");
		campaignId = reader.getAttributeAsInt("id");
		campaignBudgetBid = reader.getAttributeAsLong("budget");
		super.read(reader);
	}
	
	
	public void write(TransportWriter writer) {
		writer.attr("ucs", ucsBid).attr("id", campaignId).attr("budget", campaignBudgetBid);
		super.write(writer);
	}

	
	@Override
	public String getTransportName() {
		return "AdNetBid";
	}

}
