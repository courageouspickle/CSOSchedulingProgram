import java.util.*;

public class Shift implements java.io.Serializable, Comparable<Shift> {

  private static final long serialVersionUID = 2L;

  private String title;
  private String rankRequirement;
  private int startTime;
  private int endTime;
  private int iteration;
  private double length = 0.0;
  private String dayOfWeek;
  private double priority = 0.0;

  public Shift() {
    this.title = "";
    this.rankRequirement = "";
    this.startTime = 0;
    this.endTime = 0;
    this.iteration = 0;
    this.length = 0.0;
    this.dayOfWeek = "";
  }

  public Shift(String title, String day, int start, int end, int iteration, String rank, double priority) {
    this.title = title;
    this.rankRequirement = "";
    this.dayOfWeek = day;
    this.startTime = start;
    this.endTime = end;
    this.iteration = iteration;
    this.priority = priority;
    setRankRequirement(rank);
    updateLength();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj.getClass() != this.getClass()) {
      return false;
    }
    Shift other = (Shift) obj;
    return (this.title.equals(other.getTitle()) && this.dayOfWeek.equals(other.getDayOfWeek()) &&
           (this.startTime == other.getStart()) && (this.endTime == other.getEnd()) && (this.iteration == other.getIteration()));
  }

  @Override
  public String toString() {
    return (this.title + "_" + this.dayOfWeek + "_" + Integer.toString(this.iteration));
  }

  @Override
  public int compareTo(Shift otherShift) {
    if (this.startTime != otherShift.getStart()) {
      return this.startTime - otherShift.getStart();
    }
    else {
        return  otherShift.getEnd() - this.endTime;
    }
  }

  private void updateLength() {
    int length = 0;
    if ((getEnd() / 100) >= (getStart() / 100)) {
      length = (getEnd() / 100) - (getStart() / 100);
    }
    else {
      length = (24 - (getStart() / 100)) + (getEnd() / 100);
    }
    int min = 0;
    if ((getEnd() % 100) >= (getStart() % 100)) {
      min = (getEnd() % 100) - (getStart() % 100);
    }
    else {
      min = (60 - (getStart() % 100)) + (getEnd() % 100);
      length -= 1;
    }
    double minFrac = ((double) min) / 60.0;
    this.length = ((double) length) + minFrac;
  }

  public String getTitle() {
    return this.title;
  }

  public String getRankRequirement() {
    return this.rankRequirement;
  }

  public void setRankRequirement(String rankRequirement) {
    rankRequirement = rankRequirement.toLowerCase().trim();
    switch(rankRequirement) {
      case "pc":
      case "p":
      case "program coordinator":
      case "4":
        this.rankRequirement = "PC";
        break;
      case "supervisor":
      case "supe":
      case "s":
      case "3":
        this.rankRequirement = "Supervisor";
        break;
      case "fto":
      case "field training officer":
      case "2":
      case "f":
      case "training officer":
        this.rankRequirement = "FTO";
        break;
      case "general":
      case "cso general":
      case "general cso":
      case "cso":
      case "1":
      case "g":
      case "c":
        this.rankRequirement = "General";
        break;
      case "trainee":
      case "fte":
      case "trainee (i)":
      case "trainee (ii)":
      case "trainee (iii)":
      case "0":
      case "t":
      case "miscreant":
        this.rankRequirement = "Trainee";
        break;
      default:
        System.out.println("Unable to parse rank argument: \"" + rankRequirement + "\" for " + this.toString());
        throw new IllegalArgumentException();
    }
  }

  public void setStartTime(int time){
    this.startTime = time;
    updateLength();
  }

  public void setEndTime(int time){
    this.endTime = time;
    updateLength();
  }

  public void setPriority(double prio){
    this.priority = prio;
    return;
  }

  public double getPriority() {
    return this.priority;
  }

  public double getLength() {
    return this.length;
  }

  public int getStart() {
    return this.startTime;
  }

  public int getEnd() {
    return this.endTime;
  }

  public int getIteration() {
    return this.iteration;
  }

  public String getDayOfWeek() {
    return this.dayOfWeek;
  }

  public String view() {
    return getTitle() + " " + getDayOfWeek() + " " + getIteration() + "\nRank: "
     + getRankRequirement() + "\nHours: " + getStart() + " - " + getEnd() + "\nPriority: " + getPriority();
  }

}
