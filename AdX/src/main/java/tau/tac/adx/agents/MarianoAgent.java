/*
 * DefaultUsers.java
 *
 * COPYRIGHT  2008
 * THE REGENTS OF THE UNIVERSITY OF MICHIGAN
 * ALL RIGHTS RESERVED
 *
 * PERMISSION IS GRANTED TO USE, COPY, CREATE DERIVATIVE WORKS AND REDISTRIBUTE THIS
 * SOFTWARE AND SUCH DERIVATIVE WORKS FOR NONCOMMERCIAL EDUCATION AND RESEARCH
 * PURPOSES, SO LONG AS NO FEE IS CHARGED, AND SO LONG AS THE COPYRIGHT NOTICE
 * ABOVE, THIS GRANT OF PERMISSION, AND THE DISCLAIMER BELOW APPEAR IN ALL COPIES
 * MADE; AND SO LONG AS THE NAME OF THE UNIVERSITY OF MICHIGAN IS NOT USED IN ANY
 * ADVERTISING OR PUBLICITY PERTAINING TO THE USE OR DISTRIBUTION OF THIS SOFTWARE
 * WITHOUT SPECIFIC, WRITTEN PRIOR AUTHORIZATION.
 *
 * THIS SOFTWARE IS PROVIDED AS IS, WITHOUT REPRESENTATION FROM THE UNIVERSITY OF
 * MICHIGAN AS TO ITS FITNESS FOR ANY PURPOSE, AND WITHOUT WARRANTY BY THE
 * UNIVERSITY OF MICHIGAN OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT
 * LIMITATION THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE. THE REGENTS OF THE UNIVERSITY OF MICHIGAN SHALL NOT BE LIABLE FOR ANY
 * DAMAGES, INCLUDING SPECIAL, INDIRECT, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, WITH
 * RESPECT TO ANY CLAIM ARISING OUT OF OR IN CONNECTION WITH THE USE OF THE SOFTWARE,
 * EVEN IF IT HAS BEEN OR IS HEREAFTER ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */
package tau.tac.adx.agents;

import java.util.logging.Logger;

import se.sics.tasim.aw.Message;
import tau.tac.adx.sim.Builtin;

//Mariano
public class MarianoAgent extends Builtin {

	public static final String MARIANO_AGENT_NAME = "mariano";
	private Logger log;

	/**
	 * Default constructor.
	 */
	public MarianoAgent() {
		super(MARIANO_AGENT_NAME);
	}

	/**
	 * @see se.sics.tasim.aw.TimeListener#nextTimeUnit(int)
	 */
	@Override
	public void nextTimeUnit(int date) {
	}

	/**
	 * @see edu.umich.eecs.tac.sim.Users#setup()
	 */
	@Override
	protected void setup() {
		this.log = Logger.getLogger(MarianoAgent.class.getName());
	}

	/**
	 * @see Builtin#stopped()
	 */
	@Override
	protected void stopped() {
	}

	/**
	 * @see Builtin#shutdown()
	 */
	@Override
	protected void shutdown() {
	}

	/**
	 * @see se.sics.tasim.aw.Agent#messageReceived(se.sics.tasim.aw.Message)
	 */
	@Override
	protected void messageReceived(Message message) {
	}

}
