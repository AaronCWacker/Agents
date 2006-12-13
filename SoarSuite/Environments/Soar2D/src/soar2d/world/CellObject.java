package soar2d.world;

import java.util.*;

import soar2d.*;
import soar2d.player.Player;

/**
 * @author voigtjr
 *
 * This class represents contents of cells. Other special meta-objects are 
 * also used, like a redraw object to trigger the redrawing of a cell, or
 * an explosion object to trigger the drawing of an explosion.
 */
public class CellObject {
	/**
	 * Property list, name to value, both strings for simplicity
	 */
	HashMap<String, String> properties = new HashMap<String, String>();
	/**
	 * List of properties to apply to the object if this object is applied.
	 * Used for YJs boxes.
	 */
	HashMap<String, String> propertiesApply = new HashMap<String, String>();
	/**
	 * The name of this cell object.
	 */
	String name;
	/**
	 * If this object should be updated.
	 */
	boolean updatable;
	/**
	 * If this object should be consumed after an update.
	 */
	boolean consumable;

	/**
	 * add the points property to the player during the apply.
	 * Points can be negative or zero.
	 */
	boolean pointsApply = false;
	/**
	 * add the missiles to the player during the apply.
	 * Missiles can be negative or zero.
	 */
	boolean missilesApply = false;
	/**
	 * add the health to the player during the apply.
	 * Health can be negative or zero.
	 */
	boolean healthApply = false;
	/**
	 * If this is true, only apply health if shields are down.
	 * Useful for attacking missiles.
	 */
	boolean healthApplyShieldsDown = false;
	/**
	 * add the energy to the player during the apply.
	 * energy can be negative or zero.
	 */
	boolean energyApply = false;
	/**
	 * If this is true, only apply energy if shields are up.
	 * Useful for attacking missiles.
	 */
	boolean energyApplyShieldsUp = false;
	/**
	 * Apply the decay property to this cell's points on an update.
	 */
	boolean decayUpdate = false;
	
	public CellObject(CellObject cellObject) {
		this.properties = new HashMap<String, String>(cellObject.properties);
		this.propertiesApply = new HashMap<String, String>(cellObject.propertiesApply);
		this.name = new String(cellObject.name);
		this.updatable = cellObject.updatable;
		this.consumable = cellObject.consumable;
		this.pointsApply = cellObject.pointsApply;
		this.missilesApply = cellObject.missilesApply;
		this.healthApply = cellObject.healthApply;
		this.healthApplyShieldsDown = cellObject.healthApplyShieldsDown;
		this.energyApply = cellObject.energyApply;
		this.energyApplyShieldsUp = cellObject.energyApplyShieldsUp;
		this.decayUpdate = cellObject.decayUpdate;
	}
	
	public CellObject(String name, boolean updatable, boolean consumable) {
		this.name = name;
		this.updatable = updatable;
		this.consumable = consumable;
	}
	
	public String getName() {
		return name;
	}
	public boolean updatable() {
		return updatable;
	}
	
	/**
	 * @param name property name
	 * @param value property value
	 * 
	 * Add a property apply, that is, a property that is added to this object
	 * when it is applied.
	 * Note, this overwrites an existing property if necessary.
	 */
	public void addPropertyApply(String name, String value) {
		propertiesApply.put(name, value);
	}
	
	/**
	 * @param name property name
	 * @param value property value
	 * 
	 * Add a property to this object.
	 * Note, this overwrites an existing property if necessary.
	 */
	public void addProperty(String name, String value) {
		properties.put(name, value);
	}
	
	public void setPointsApply(boolean setting) {
		pointsApply = setting;
	}
	
	public void setMissilesApply(boolean setting) {
		missilesApply = setting;
	}
	
	/**
	 * @see energyApply
	 * @see energyApplyShieldsUp
	 */
	public void setEnergyApply(boolean setting, boolean onShieldsUp) {
		this.energyApply = setting;
		this.energyApplyShieldsUp = onShieldsUp;
	}
	
	/**
	 * @see healthApply
	 * @see healthApplyShieldsDown
	 */
	public void setHealthApply(boolean setting, boolean onShieldsDown) {
		this.healthApply = setting;
		this.healthApplyShieldsDown = onShieldsDown;
	}
	
	public void setDecayUpdate(boolean setting) {
		decayUpdate = setting;
	}
	
	/**
	 * @param player called when this player acted on this object for whatever reason
	 * @return true if the object should be removed from the cell after the apply
	 */
	public boolean apply(Player player) {
		if (propertiesApply.size() > 0) {
			Iterator<String> iter = propertiesApply.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				String value = propertiesApply.get(key);
				Soar2D.logger.info("Box opened, new property: " + key + " --> " + value);
				properties.put(key, value);
			}
		}
		
		if (pointsApply) {
			assert properties.containsKey(Names.kPropertyPoints);
			int points = Integer.parseInt(properties.get(Names.kPropertyPoints));
			player.adjustPoints(points, name);
		}
		
		// TODO: implement
		assert missilesApply == false;
		assert energyApply == false;
		assert energyApplyShieldsUp == false;
		assert healthApply == false;
		assert healthApplyShieldsDown == false;
		
		return consumable;	// if this is true the object is removed from 
							// the cell after the apply
	}
	/**
	 * @param world the world 
	 * @param location where this object is at
	 * @return true if the object should be removed from the cell after the update
	 * 
	 * This is called if the object is "updatable"
	 */
	public boolean update(World world, java.awt.Point location) {
		if (decayUpdate) {
			assert properties.containsKey(Names.kPropertyPoints);
			int points = Integer.parseInt(properties.get(Names.kPropertyPoints));
			points -= 1;
			properties.put(Names.kPropertyPoints, Integer.toString(points));
			if (points == 0) {
				return true;	// this causes this object to be removed from the cell
			}
		}
		return false; // this keeps this object around
	}
	
	/**
	 * @param name the property to check for
	 * @return true if that property exists
	 * 
	 * Note: property foo -> false returns true here, this doesn't return the value
	 * of the property, but rather its existence.
	 */
	public boolean hasProperty(String name) {
		if (properties.containsKey(name)) {
			return true;
		}
		return false;
	}
	
	/**
	 * @return a set of property names
	 */
	public Set<String> getPropertyNames() {
		return properties.keySet();
	}
	/**
	 * @param name the property name
	 * @return the property value or null if it doesn't exist
	 */
	public String getProperty(String name) {
		if (properties.containsKey(name)) {
			return (String)properties.get(name);
		}
		return null;
	}
	/**
	 * @param name the property
	 * @return the string converted to a boolean
	 * 
	 * same as getProperty but converts it to boolean first
	 * use with care
	 */
	public boolean getBooleanProperty(String name) {
		if (properties.containsKey(name)) {
			return Boolean.parseBoolean((String)properties.get(name));
		}
		return Soar2D.config.kDefaultPropertyBoolean;
	}
	/**
	 * @param name the property
	 * @return the string converted to a int
	 * 
	 * same as getProperty but converts it to int first
	 * use with care
	 */
	public int getIntProperty(String name) {
		if (properties.containsKey(name)) {
			return Integer.parseInt((String)properties.get(name));
		}
		return Soar2D.config.kDefaultPropertyInt;
	}
	/**
	 * @param name the property
	 * @return the string converted to a float
	 * 
	 * same as getProperty but converts it to float first
	 * use with care
	 */
	public float getFloatProperty(String name) {
		if (properties.containsKey(name)) {
			return Float.parseFloat((String)properties.get(name));
		}
		return Soar2D.config.kDefaultPropertyFloat;
	}
}
