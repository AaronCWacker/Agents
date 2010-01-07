package edu.umich.soar.sproom.soar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import lcmtypes.laser_t;
import sml.Agent;
import sml.Identifier;
import edu.umich.soar.FloatWme;
import edu.umich.soar.IntWme;
import edu.umich.soar.sproom.Adaptable;
import edu.umich.soar.sproom.SharedNames;
import edu.umich.soar.sproom.command.CommandConfig;
import edu.umich.soar.sproom.command.Lidar;

public class LidarIL implements InputLinkElement {
	private static final Log logger = LogFactory.getLog(LidarIL.class);

	private final CommandConfig c = CommandConfig.CONFIG;

	private class Range {
		private final Identifier rangewme;
		private final FloatWme distancewme;
		private boolean valid = true;
		
		private Range(Agent agent, int id, double relativeBearing) {
			rangewme = agent.CreateIdWME(root, SharedNames.RANGE);
			IntWme.newInstance(rangewme, SharedNames.ID, id);
			YawWme.newInstance(rangewme, SharedNames.RELATIVE_BEARING, relativeBearing);
			distancewme = FloatWme.newInstance(rangewme, SharedNames.DISTANCE);
			
			logger.debug(String.format("Created range %d relative-bearing %1.3f", id, relativeBearing));
		}
		
		private void update(double distance) {
			if (!valid) {
				throw new IllegalStateException();
			}
			distancewme.update(distance);
		}
		
		private void destroy() {
			valid = false;
			rangewme.DestroyWME();
		}
	}
	
	private final Identifier root;
	private Range[] ranges;

	LidarIL(Identifier root, Adaptable app) {
		this.root = root;
		ranges = new Range[0];
	}
	
	private void createRanges(Adaptable app, laser_t laser) {
		Agent agent = (Agent)app.getAdapter(Agent.class);
	
		for (Range range : ranges) {
			range.destroy();
		}
		
		ranges = new Range[c.getRangeCount()];
		double relativeBearing = laser.rad0;
		for (int index = 0; index < ranges.length; ++index, relativeBearing += laser.radstep) {
			ranges[index] = new Range(agent, index - (ranges.length / 2), relativeBearing);
		}
	}

	@Override
	public void update(Adaptable app) {
		Lidar lidar = (Lidar)app.getAdapter(Lidar.class);
		laser_t laser = lidar.getLaserLowRes();
		if (laser == null) {
			return;
		}
		
		if (laser.nranges != ranges.length) {
			createRanges(app, laser);
		}
		
		for(int index = 0; index < laser.nranges; ++index) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("range index %d distance update %1.3f", index, laser.ranges[index]));
			}
			ranges[index].update(laser.ranges[index]);
		}
	}

}