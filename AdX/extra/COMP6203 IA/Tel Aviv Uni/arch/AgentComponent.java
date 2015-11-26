package arch;

import se.sics.isl.util.ConfigManager;

/**
 * AgentComponent is the base class for the functionality components of
 * a Tau Agent. All should implement the IAgentComponent interface
 * 
 * @author Mariano Schain
 */
public abstract class AgentComponent implements IAgentComponent {
	
	protected IAgent aaAgent;
	protected ConfigManager aaConfig;
		
	public void setAgent(IAgent a) {
		aaAgent = a;
	}
	
	public void setConfigManager(ConfigManager c) {
		aaConfig = c;
	}
	
}
