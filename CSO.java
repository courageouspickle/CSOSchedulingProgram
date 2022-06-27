import java.util.*;

public class CSO implements java.io.Serializable {

  private static final long serialVersionUID = 1L;

  private int badgeNumber;
  private String name;
  private String rank;
  private double hoursRequested;
  private double hoursScheduled;
  private List<Shift> possShifts;
  private List<Shift> schedShifts;
  private HashMap<Shift, Integer> schedRequest;

  public CSO(int badgeNumber) {
    this.badgeNumber = badgeNumber;
    this.name = "";
    this.rank = "";
    this.hoursRequested = 0.0;
    this.hoursScheduled = 0.0;
    this.schedShifts = new ArrayList<Shift>();
    this.schedRequest = new HashMap<Shift, Integer>();
    this.possShifts = new ArrayList<Shift>();
  }

  public String toString() {
    return "CSO #" + Integer.toString(this.badgeNumber);
  }

  public String view() {
    return("CSO " + this.name + " #" + this.badgeNumber + "\nRank: " + this.rank
     + "\nHours Requested: " + this.hoursRequested);
  }

  public int getBadge() {
    return this.badgeNumber;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRank() {
    return this.rank;
  }

//change input string to lowercase and remove trailing and preceeding whitespace
//catch as many synonyms for each rank as possible
  public void setRank(String rank) {
    rank = rank.toLowerCase().trim();
    switch(rank) {
      case "pc":
      case "p":
      case "program coordinator":
      case "4":
        this.rank = "PC";
        break;
      case "supervisor":
      case "supe":
      case "s":
      case "3":
        this.rank = "Supervisor";
        break;
      case "fto":
      case "field training officer":
      case "2":
      case "f":
      case "training officer":
        this.rank = "FTO";
        break;
      case "general":
      case "cso general":
      case "general cso":
      case "cso":
      case "1":
      case "g":
      case "c":
        this.rank = "General";
        break;
      case "trainee":
      case "fte":
      case "trainee (i)":
      case "trainee (ii)":
      case "trainee (iii)":
      case "0":
      case "t":
      case "miscreant":
        this.rank = "Trainee";
        break;
      default:
        System.out.println("Unable to parse rank argument: \"" + rank + "\" for " + this.toString());
        throw new IllegalArgumentException();
    }
  }

  public double getHoursReq() {
    return this.hoursRequested;
  }

  public void setHoursReq(double num) {
    this.hoursRequested = num;
  }

  public double getHoursSched() {
    return this.hoursScheduled;
  }

  public void setHoursSched(double num) {
    this.hoursScheduled = num;
  }

  public List<Shift> getPossShifts() {
    return this.possShifts;
  }

  public void setPossShifts(List<Shift> possibleShifts) {
    this.possShifts = prioritySort(possibleShifts);
  }

  private List<Shift> prioritySort(List<Shift> shifts) {
    List<Shift> sorted = new ArrayList<Shift>();
    for (int i = 0; i < shifts.size(); i++) {
      double max = 0;
      Shift best = shifts.get(0);
      for (Shift shift : shifts) {
        if (shift.getPriority() > max) {
          max = shift.getPriority();
          best = shift;
        }
      }
      sorted.add(best);
      shifts.remove(best);
    }
    return sorted;
  }

  public List<Shift> getSchedShifts() {
    return this.schedShifts;
  }

  public void initSchedule() {
    this.schedShifts = new ArrayList<Shift>();
    setHoursSched(0.0);
  }

  public HashMap<Shift, Integer> getSchedRequest() {
    return this.schedRequest;
  }

  public void setSchedRequest(HashMap<Shift, Integer> scheduleRequest) {
    this.schedRequest = scheduleRequest;
  }

  public void addSchedShift(Shift shift) {
    this.schedShifts.add(shift);
    this.hoursScheduled += shift.getLength();
  }

  public void removeSchedShift(Shift shift) {
    this.schedShifts.remove(shift);
    this.hoursScheduled -= shift.getLength();
  }

  public void addPossShift(Shift shift) {
    this.possShifts.add(shift);
  }

  public void removePossShift(Shift shift) {
    this.possShifts.remove(shift);
  }

}
