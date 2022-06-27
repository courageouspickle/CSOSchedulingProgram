import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class WriteToSpreadsheet {

  List<String> daysOfTheWeek;
  List<String> shiftTitles;

  public WriteToSpreadsheet(HashMap<Shift, CSO> oldSchedule) {

    this.daysOfTheWeek = Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
    this.shiftTitles = Arrays.asList("RSF AM", "RSF PM", "UHall", "BWW", "Bechtel", "Supervisor", "Dispatch",
                                             "Early Escort", "Blackwell", "Late Escort", "CKC", "FH", "Housing IC");

    try (PrintWriter writer = new PrintWriter("Schedule.csv")) {
      HashMap<String, String> schedule = objectsToStrings(oldSchedule);
      StringBuilder sb = new StringBuilder();
      sb.append("Shift / Day");
      for (String day : daysOfTheWeek) {
        sb.append("," + day);
      }
      sb.append('\n');
      for (String title : shiftTitles) {
        if (title.equals("Early Escort") || title.equals("Late Escort")) {
          for (int i = 1; i <= 3; i++) {
            makeRow(schedule, title, sb, i);
          }
        }
        else if (title.equals("CKC") || title.equals("FH")) {
          for (int i = 1; i <= 2; i++) {
            makeRow(schedule, title, sb, i);
          }
        }
        else {
          makeRow(schedule, title, sb, 1);
        }
      }
      writer.write(sb.toString());
    } catch (FileNotFoundException e) {
      System.out.println(e.getMessage());
    }
  }

  private HashMap<String, String> objectsToStrings(HashMap<Shift, CSO> schedule) {
    HashMap<String, String> newSchedule = new HashMap<String, String>();
    List<Shift> shifts = new ArrayList<Shift>(schedule.keySet());
    for (Shift shift : shifts) {
      String title;
      String badge;
      title = shift.getTitle() + "_" + shift.getDayOfWeek() + "_" + Integer.toString(shift.getIteration());
      badge = Integer.toString(schedule.get(shift).getBadge());
      newSchedule.put(title, badge);
    }
    return newSchedule;
  }

  private void makeRow(HashMap<String, String> schedule, String title, StringBuilder sb, int iteration) {
    if (iteration == 1) {
      sb.append(title);
    }
    else {
      sb.append(title + "_" + Integer.toString(iteration));
    }
    for (int i = 0; i < 7; i++) {
      String fullShiftTitle = title + "_" + daysOfTheWeek.get(i) + "_" + Integer.toString(iteration);
      if (schedule.containsKey(fullShiftTitle)) {
        sb.append("," + schedule.get(fullShiftTitle));
      }
      else {
        sb.append(",");
      }
    }
    sb.append("\n");
  }

}
