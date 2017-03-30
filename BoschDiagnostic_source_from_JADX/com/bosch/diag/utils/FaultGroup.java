package com.bosch.diag.utils;

import java.util.ArrayList;

public class FaultGroup {
    public ArrayList<Fault> childFaults;
    public String en;
    public String lan;
    public String zh;

    public FaultGroup() {
        this.childFaults = new ArrayList();
    }

    public void addFault(Fault fault) {
        this.childFaults.add(fault);
    }

    public String toString() {
        return "fault group = " + this.zh + " child size = " + this.childFaults.size();
    }

    public String getName() {
        String name = "";
        if ("zh".equals(this.lan)) {
            return this.zh;
        }
        return this.en;
    }
}
