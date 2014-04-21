package com.pbm;

import java.io.Serializable;
import java.lang.String;

public class Machine implements Serializable {
	private static final long serialVersionUID = 1L;
	public int machineNo;
	public String name;
	public int numLocations;
	public String condition;
	public String conditionDate;
	public boolean existsInRegion;

	public Machine(String newName) {
		name = newName;
	}

	public Machine(int newMachineNo, String newName) {
		machineNo = newMachineNo;
		name = newName;
	}

	public Machine(int newMachineNo, String newName, boolean newExistsInRegion) {
		machineNo = newMachineNo;
		name = newName;
		existsInRegion = newExistsInRegion;
	}

	public Machine(int newMachineNo, String newName, int newNumLocations) {
		machineNo = newMachineNo;
		name = newName;
		numLocations = newNumLocations;
	}
	
	public Machine(int newMachineNo, String newName, int newNumLocations, String newCondition, String newConditionDate) {
		machineNo = newMachineNo;
		name = newName;
		numLocations = newNumLocations;
		condition = newCondition;
		conditionDate = newConditionDate;
	}
	
	public void setNumLocations(int newNumLocations) {
		numLocations = newNumLocations;
	}
	
	public void setExistsInRegion(boolean newExistsInRegion) {
		existsInRegion = newExistsInRegion;
	}
	
	public String toString() {
		return name;
	}
}