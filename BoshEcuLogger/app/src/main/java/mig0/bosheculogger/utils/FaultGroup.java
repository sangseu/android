package mig0.bosheculogger.utils;

import java.util.ArrayList;

/**
 * Created by mig-7 on 3/28/2017.
 */

public class FaultGroup {
    public ArrayList<Fault> childFaults = new ArrayList<Fault>();
    public String en;
    public String lan;
    public String zh;

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
