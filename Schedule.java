import java.io.File;
import java.util.*;

public class Schedule {

  private List<Shift> allShifts;
  private List<CSO> allCSOs;
  private HashMap<Shift, CSO> schedule;
  private double avgShiftLength = 6.0;
  private int counter = 0;

  public Schedule(List<Shift> allShifts, List<CSO> allCSOs) {
      // Initializing the lists of employees and shifts as Lists.
    this.allCSOs = allCSOs;
    this.allShifts = allShifts;
    this.schedule = backtrackSearch();
    for (CSO cso : allCSOs) {
      List<Shift> schedShifts = cso.getSchedShifts();
      int runningTotal = 0;
      for (Shift shift : schedShifts) {
        runningTotal += cso.getSchedRequest().get(shift);
      }
      Double avgScore = (1.0 * runningTotal) / schedShifts.size();
      System.out.println(cso + " requested: " + cso.getHoursReq() + " hours and was scheduled for: " + cso.getHoursSched() + " hours with an average score: " + Double.toString(avgScore));
    }
  }

    private HashMap<Shift, CSO> backtrackSearch() {
      HashMap<Shift, CSO> empty = new HashMap<Shift, CSO>();
      return this.recursiveBacktracking(empty, 0);
    }

    private HashMap<Shift, CSO> recursiveBacktracking(HashMap<Shift, CSO> assignment, int index) {
      if (isComplete()) {
        return assignment;
      }
      CSO currentCSO = this.allCSOs.get(index);
      for (int i = 0; i < this.allCSOs.size(); i++) {
        double schedHours = currentCSO.getHoursSched();
        double maxHours = currentCSO.getHoursReq();
        if ((schedHours < (maxHours - this.avgShiftLength / 2.0)) && !currentCSO.getPossShifts().isEmpty()) {
          break;
        }
        else {
          index = (index + 1) % this.allCSOs.size();
          currentCSO = this.allCSOs.get(index);
        }
      }
      updatePossShifts(currentCSO, assignment);
      List<Shift> possShifts = currentCSO.getPossShifts();
      for (int i = 0; i < possShifts.size(); i++) {
        Shift shift = possShifts.get(i);
        assignShift(currentCSO, shift, assignment);
        index = (index + 1) % (this.allCSOs.size());
        HashMap<Shift, CSO> newAssignment = recursiveBacktracking(assignment, index);
        if (!(newAssignment.isEmpty())) {
            return newAssignment;
        }
        this.counter += 1;
        System.out.println(this.counter);
        unassignShift(currentCSO, shift, assignment);
      }
      HashMap<Shift, CSO> empty = new HashMap<Shift, CSO>();
      return empty;
    }


    // Here we check to see if the schedule is finished being generated. It checks
    // if the currently built schedule is valid and is as complete as possible.
    // First it checks if every shift is staffed; if so then it is complete.
    // If there are shifts available and CSOs wanting more hours, it checks if
    // there exists an unstaffed shift such that there exists a CSO who can work it
    // and they want more hours. If such a shift exists, the schedule is NOT complete.

    private Boolean isComplete() {
      if (this.allShifts.isEmpty()) {
        return true;
      }
      for (CSO cso : allCSOs) {
        for (Shift possibleShift : allShifts) {
          if (!(checkRank(cso, possibleShift) && cso.getSchedRequest().keySet().contains(possibleShift))) {
            continue;
          }
          for (Shift scheduledShift : cso.getSchedShifts()) {
            if (possibleShift.getPriority() > scheduledShift.getPriority()) {
              return false;
            }
          }
        }
      }

      for (Shift shift : allShifts) {
        for (CSO cso : allCSOs) {
          List<Shift> possShifts = cso.getPossShifts();
          if (possShifts.contains(shift) && !hourConstraint(cso, shift)) {
            return false;
          }
        }
      }

      return true;
    }

    private void updatePossShifts(CSO cso, HashMap<Shift, CSO> assignment) {
      HashMap<Shift, Integer> schedRequest = cso.getSchedRequest();
      HashMap<Shift, Double> prioritySchedRequest = new HashMap<Shift, Double>();
      List<Shift> oldPossShifts = new ArrayList<Shift>(schedRequest.keySet());
      List<Shift> newPossShifts = new ArrayList<Shift>();
      List<Shift> scheduledShifts = new ArrayList<Shift>(assignment.keySet());
      for (Shift shift : scheduledShifts) {
        oldPossShifts.remove(shift);
      }
      for (Shift shift : oldPossShifts) {
        if (!conflictExists(cso, shift)) {
          Double n = 1.0 * schedRequest.get(shift);
          Double m = 1.0;
          for (CSO otherCSO : this.allCSOs) {
            if (!otherCSO.equals(cso)) {
              List<Shift> otherPossShifts = otherCSO.getPossShifts();
              if (otherPossShifts.contains(shift)) {
                m += 1.0;
              }
            }
          }
          Double value = (1.0 - (n / 10.0)) + 2.0 * (0.9 / (m - 0.9));
          Double scaledValue = 2.0 / (1.0 + Math.exp(value));
          prioritySchedRequest.put(shift, scaledValue);
          Boolean shiftAdded = false;
          for (int i = 0; i < newPossShifts.size(); i++) {
            if (prioritySchedRequest.get(shift) < prioritySchedRequest.get(newPossShifts.get(i))) {
              newPossShifts.add(i, shift);
              shiftAdded = true;
              break;
            }
            else if (prioritySchedRequest.get(shift) == prioritySchedRequest.get(newPossShifts.get(i))) {
              if (m == 1) {
                newPossShifts.add(i, shift);
                shiftAdded = true;
                break;
              }
            }
          }
          if (!shiftAdded) {
            newPossShifts.add(shift);
          }
        }
      }
      cso.setPossShifts(newPossShifts);
    }

    // The following methods determine whether there is a contraint existing between a CSOs
    // already scheduled shifts and the given shift. It calls a series of other methods which
    // individually check a single constraint.
    private Boolean conflictExists(CSO cso, Shift shift) {
      Boolean hourly = this.hourConstraint(cso, shift);
      Boolean overlap = this.shiftOverlap(cso, shift);
      return (hourly || overlap);
    }

    private Boolean hourConstraint(CSO cso, Shift shift) {
      double maxHours = cso.getHoursReq();
      double currHours = cso.getHoursSched();
      double shiftLength = shift.getLength();
      if ((shiftLength + currHours) <= (maxHours + (this.avgShiftLength / 2.0))) {
        return false;
      }
      else {
        return true;
      }
    }

    private Boolean shiftOverlap(CSO cso, Shift shift) {
      HashMap<String, Integer> daysOfTheWeek = new HashMap<>();
      daysOfTheWeek.put("Sunday", 1);
      daysOfTheWeek.put("Monday", 2);
      daysOfTheWeek.put("Tuesday", 3);
      daysOfTheWeek.put("Wednesday", 4);
      daysOfTheWeek.put("Thursday", 5);
      daysOfTheWeek.put("Friday", 6);
      daysOfTheWeek.put("Saturday", 7);
      List<Shift> schedShifts = cso.getSchedShifts();
      for (Shift tempShift : schedShifts) {
        int diffOfDays = daysOfTheWeek.get(tempShift.getDayOfWeek()) - daysOfTheWeek.get(shift.getDayOfWeek());
        int tempStart = tempShift.getStart();
        int tempEnd = tempShift.getEnd();
        int currentStart = shift.getStart();
        int currentEnd = shift.getEnd();
        if ((diffOfDays == -1) && (tempEnd < tempStart)) {
          if (currentStart < tempEnd) {
            return true;
          }
        }
        else if (diffOfDays == 0) {
          if (tempEnd < tempStart) {
            tempEnd += 2400;
          }
          if (currentEnd < currentStart) {
            currentEnd += 2400;
          }
          if (tempStart < currentEnd && currentStart < tempEnd) {
            return true;
          }
        }
        else if ((diffOfDays == 1) && (currentEnd < currentStart)) {
          if (tempStart < currentEnd) {
            return true;
          }
        }
        else {
          continue;
        }
      }
      return false;
    }

//=============== UTILITY FUNCTIONS ============================

    private boolean checkRank(CSO cso, Shift shift) {
      if (cso.getRank() == null) {
        System.out.println("WARNING: CSO #" + cso.getBadge() +
        " does not have an associated rank, so they will not be assigned any shifts. \n(Try the \"modify\" command.)");
        return false;
      }
      switch (shift.getRankRequirement()) {
        case "Trainee":
          return cso.getRank().equals("Trainee");
        case "General":
          return (cso.getRank().equals("General") || cso.getRank().equals("FTO") || cso.getRank().equals("Supervisor") || cso.getRank().equals("PC"));
        case "FTO":
          return (cso.getRank().equals("FTO") || cso.getRank().equals("Supervisor") || cso.getRank().equals("PC"));
        case "Supervisor":
          return (cso.getRank().equals("Supervisor") || cso.getRank().equals("PC"));
        case "PC":
          return cso.getRank().equals("PC");
        default:
          System.out.println("Shift rank requirement: \"" + shift.getRankRequirement() + "\" invalid for shift " + shift.getTitle());
          throw new IllegalArgumentException();
      }
    }

    private void assignShift(CSO cso, Shift shift, HashMap<Shift, CSO> assignment) {
      assignment.put(shift, cso);
      cso.addSchedShift(shift);
      this.allShifts.remove(shift);
      for (CSO tempCSO : this.allCSOs) {
        tempCSO.removePossShift(shift);
      }
      for (Shift tempShift : this.allShifts) {
        if (conflictExists(cso, tempShift)) {
          cso.removePossShift(tempShift);
        }
      }
      //updatePossShifts(cso, assignment);
    }

    private void unassignShift(CSO cso, Shift shift, HashMap<Shift, CSO> assignment) {
      assignment.remove(shift, cso);
      cso.removeSchedShift(shift);
      this.allShifts.add(shift);
      for (CSO tempCSO : this.allCSOs) {
        if (tempCSO.getSchedRequest().containsKey(shift)) {
          tempCSO.addPossShift(shift);
        }
      }
      /*
      List<Shift> newPossShifts = new ArrayList<Shift>(cso.getSchedRequest().keySet());
      for (Shift tempShift : this.allShifts) {
        if (assignment.keySet().contains(tempShift) || conflictExists(cso, tempShift)) {
          newPossShifts.remove(tempShift);
        }
      }
      */
      //updatePossShifts(cso, assignment);
    }

    public HashMap<Shift, CSO> getSchedule() {
      return this.schedule;
    }

  }
