import java.util.*;
import java.io.*;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class generateSchedule {

  static final File CWD = new File(".");
  static final File DATA = new File(CWD, "data");
  static final File CSO_PATH = new File(DATA, "CSOs");
  static final File SHIFT_PATH = new File(DATA, "Shifts");
  static final File SCHEDULE_REQUEST_PATH = new File(CWD, "requests");

  public static void main(String... args) throws IOException {
    if (args.length == 0) {
      System.out.println("Please enter a command.");
      System.exit(0);
    }
    String[] allowed = {"build", "create", "modify", "update", "delete", "view"};
    List<String> commands = new ArrayList<>();
    commands.addAll(Arrays.asList(allowed));
    if (!commands.contains(args[0])) {
      System.out.println("No command with that name exists.");
      System.exit(0);
    }
    File[] listOfShifts = SHIFT_PATH.listFiles();
    File[] listOfCSOs = CSO_PATH.listFiles();
    switch (args[0]) {
      case "build":
        List<Shift> allShifts = new ArrayList<Shift>();
        List<CSO> allCSOs = new ArrayList<CSO>();
        for (File shiftFile : listOfShifts) {
          Shift tempShift = readObject(shiftFile, Shift.class);
          allShifts.add(tempShift);
        }
        for (File csoFile : listOfCSOs) {
          CSO tempCSO = readObject(csoFile, CSO.class);
          if (tempCSO.getHoursReq() > 0.0) {
            allCSOs.add(tempCSO);
          }
        }
        Schedule temp = new Schedule(allShifts, allCSOs);
        HashMap<Shift, CSO> schedule = temp.getSchedule();
        for (Shift shift : allShifts) {
          File shiftFile = new File(SHIFT_PATH, (shift.getTitle() + "_" + shift.getDayOfWeek() + "_" + Integer.toString(shift.getIteration())));
          byte[] serializedShift = serialize(shift);
          writeContents(shiftFile, serializedShift);
        }
        for (CSO cso : allCSOs) {
          File csoFile = new File(CSO_PATH, Integer.toString(cso.getBadge()));
          byte[] serializedCSO = serialize(cso);
          writeContents(csoFile, serializedCSO);
        }
        if (schedule.isEmpty()) {
          System.out.println("Error! The schedule was unable to be created.");
        }
        else {
          writeToSpreadsheet(schedule);
          System.out.println("The schedule was successfully created.");
        }
        break;
      case "create":
        if (args.length < 2) {
          System.out.println("You must specify which data item you wish to create. (CSO or Shift)");
        }
        else if (args[1].equals("shift") || args[1].equals("CSO")) {
          if (args[1].equals("shift")) {
            String shiftTitle = args[2];
            String dayOfWeek = args[3];
            int startTime = Integer.parseInt(args[4]);
            int endTime = Integer.parseInt(args[5]);
            String rank = args[6];
            double priority = Double.parseDouble(args[7]);
            int iter = 1;
            if (args.length > 8) { iter = Integer.parseInt(args[8]); }
            for (; iter > 0; iter--) {
              String shiftFilename = shiftTitle + "_" + dayOfWeek + "_" + iter;
              File shiftFile = new File(SHIFT_PATH, shiftFilename);
              boolean duplicate = false;
              for (File tempShiftFile : listOfShifts) {
                if (tempShiftFile.equals(shiftFile)) {
                  duplicate = true;
                  break;
                }
              }
              if (duplicate) {
                System.out.println("There already exists a shift on that day with that title. If you wish to create a duplicate of that shift, please pass in the next iteration as an argument.");
              }
              else {
                Shift newShift = new Shift(shiftTitle, dayOfWeek, startTime, endTime, iter, rank, priority);
                byte[] serializedShift = serialize(newShift);
                writeContents(shiftFile, serializedShift);
                System.out.println("Successfully created shift: " + newShift.view());
              }
            }
          }
          else if (args[1].equals("CSO") && args.length == 3) {
            int badge = Integer.parseInt(args[2]);
            String csoFilename = args[2];
            File csoFile = new File(CSO_PATH, csoFilename);
            boolean duplicate = false;
            for (File tempCSOFile : listOfCSOs) {
              if (tempCSOFile.equals(csoFile)) {
                duplicate = true;
                break;
              }
            }
            if (duplicate) {
              System.out.println("There already exists an employee with badge number " + csoFilename);
            }
            else {
              CSO newCSO = new CSO(badge);
              byte[] serializedCSO = serialize(newCSO);
              writeContents(csoFile, serializedCSO);
              System.out.println("Successfully created CSO: " + newCSO.view());
            }
          }
          else {
            System.out.println("Incorrect number of arguments given.");
          }
        }
        else {
          System.out.println("Incorrect operands given.");
        }
        break;
      case "modify":
        if (args.length < 2) {
          System.out.println("You must specify which data item you wish to modify. (CSO or Shift)");
        }
        if (args[1].equals("shift")) {
          modifyShift(args);
        }
        else if (args[1].equals("CSO")) {
          if (args.length != 5) {
            System.out.println("Incorrect number of arguments given.");
          }
          else {
            modifyCSO(args);
          }
        }
        else {
          System.out.println("Incorrect operands given.");
        }
        break;
      case "update":
        for (File csoFile : listOfCSOs) {
          CSO cso = readObject(csoFile, CSO.class);
          cso.initSchedule();
          System.out.println("\nCSO #" + cso.getBadge() + " " + cso.getRank());
          HashMap<Shift, Integer> schedRequest = scheduleRequest(cso);
          if (!schedRequest.isEmpty()) {
            System.out.println("Updated successfully.");
          }
          else {
            System.out.println("No schedule request or invalid entries.");
          }
          cso.setSchedRequest(schedRequest);
          cso.setPossShifts(new ArrayList<Shift>(schedRequest.keySet()));
          byte[] serializedCSO = serialize(cso);
          writeContents(csoFile, serializedCSO);
        }
        createFTEShifts();
        System.out.println("Update completed.");
        break;

      case "delete":
        if (args.length < 2) {
          System.out.println("You must specify which data item you wish to delete. (CSO or Shift)");
          System.exit(0);
        }
        if (args[1].equals("shift")) {
          deleteShift(args);
          System.exit(0);
        }
        else if (args[1].equals("CSO")) {
          if (args.length != 3) {
            System.out.println("Incorrect number of arguments given.");
            System.exit(0);
          }
          else {
            deleteCSO(args[2]);
            System.exit(0);
          }
        }
        else {
          System.out.println("Incorrect operands given.");
          System.exit(0);
        }
        break;

      case "view":
        if (args.length < 2) {
          System.out.println("You must specify which data item you wish to view. (CSO or Shift)");
          System.exit(0);
        }
        if (args[1].equals("shift")) {
          viewShift(args[2]);
          System.exit(0);
        }
        else if (args[1].equals("CSO")) {
          viewCSO(args[2]);
          System.exit(0);
        }
        else {
          System.out.println("Incorrect operands given.");
          System.exit(0);
        }
        break;
    }
  }

  static void viewCSO(String badge) {
    File csoFilename = new File(CSO_PATH, badge);
    if (!csoFilename.isFile()) {
      System.out.println("There does not exist an employee with the badge number " + badge);
    }
    else {
      CSO cso = readObject(csoFilename, CSO.class);
      System.out.println(cso.view());
    }
  }

  static void viewShift(String fileName) {
    File shiftFile = new File(SHIFT_PATH, fileName);
    if (!shiftFile.isFile()) {
      System.out.println("There does not exist a shift with the file name: \"" + fileName + "\"");
    }
    else {
      Shift shift = readObject(shiftFile, Shift.class);
      System.out.println(shift.view());
    }
  }

  static void modifyShift(String[] args) {
    File[] listOfShifts = SHIFT_PATH.listFiles();
    String shiftTitle = args[2];
    int startTime = Integer.parseInt(args[3]);
    int endTime = Integer.parseInt(args[4]);
    String rank = args[5];
    String dayOfWeek = "";
    int iter = 0;
    double priority = 0.0;
    if (args.length > 6) { priority = Double.parseDouble(args[6]);
    if (args.length > 7) { iter = Integer.parseInt(args[7]); } }
    if (args.length > 8) { dayOfWeek = args[8]; }
    ArrayList<Shift> ShiftObjects = new ArrayList<Shift>();
    for (int i = listOfShifts.length - 1 ; i >= 0 ; i--) {
      Shift shift = readObject(listOfShifts[i], Shift.class);
      if (shift.getTitle().equals(shiftTitle) && (dayOfWeek == "" || shift.getDayOfWeek().equals(dayOfWeek)) && (iter == 0 || iter == shift.getIteration())) {
        shift.setStartTime(startTime);
        shift.setEndTime(endTime);
        shift.setRankRequirement(rank);
        shift.setPriority(priority);
        System.out.println("Modifying shift " + shift.getTitle() + " " + shift.getDayOfWeek() + " " + shift.getIteration());
        byte[] serializedShift = serialize(shift);
        writeContents(listOfShifts[i], serializedShift);
      }
    }
  }

  static void deleteShift(String[] args) {
    File[] listOfShifts = SHIFT_PATH.listFiles();
    String shiftTitle = args[2];
    int iter = 0;
    String dayOfWeek = "";
    if (args.length > 3) { iter = Integer.parseInt(args[3]); }
    if (args.length == 5) { dayOfWeek = args[4]; }
    for (int i = listOfShifts.length - 1 ; i >= 0 ; i--) {
      Shift shift = readObject(listOfShifts[i], Shift.class);
      if (shift.getTitle().equals(shiftTitle) && (dayOfWeek == "" || shift.getDayOfWeek().equals(dayOfWeek)) && (iter == 0 || iter == shift.getIteration())) {
        System.out.println("Deleting shift " + shift.getTitle() + " " + shift.getDayOfWeek() + " " + shift.getIteration());
        listOfShifts[i].delete();
      }
    }
  }

  static void modifyCSO(String[] args) {
    String csoBadgeNumber = args[2];
    File csoFilename = new File(CSO_PATH, csoBadgeNumber);
    if (!csoFilename.isFile()) {
      System.out.println("There does not exist an employee with the badge number " + csoBadgeNumber);
    }
    else {
      CSO cso = readObject(csoFilename, CSO.class);
      cso.setName(args[3]);
      cso.setRank(args[4]);
      byte[] serializedCSO = serialize(cso);
      writeContents(csoFilename, serializedCSO);
      System.out.println("Successfully modified CSO: " + cso.view());
    }
  }

  static void deleteCSO(String badge) {
    File csoFilename = new File(CSO_PATH, badge);
    if (!csoFilename.isFile()) {
      System.out.println("There does not exist an employee with the badge number " + badge);
    }
    else {
      csoFilename.delete();
      System.out.println("Successfully deleted CSO " + badge);
    }
  }

  static void createFTEShifts() {
    File[] listOfShifts = SHIFT_PATH.listFiles();
    File[] listOfCSOs = CSO_PATH.listFiles();
    ArrayList<CSO> csoObjects = new ArrayList<CSO>();
    for (int i = listOfShifts.length - 1 ; i >= 0 ; i--) {     //first, delete all the existing FTE shifts
      Shift shift = readObject(listOfShifts[i], Shift.class);
      if (shift.getRankRequirement() == "Trainee") {
        listOfShifts[i].delete();
      }
    }

  }

  /* Returns a byte array containing the serialized contents of OBJ. */
  static byte[] serialize(Serializable obj) {
    try {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      ObjectOutputStream objectStream = new ObjectOutputStream(stream);
      objectStream.writeObject(obj);
      objectStream.close();
      return stream.toByteArray();
    }
    catch (IOException excp) {
      System.out.println("Error!");
      return new byte[0];
    }
  }

  /*  Return an object of type T read from FILE, casting it to EXPECTEDCLASS.
   *  Throws IllegalArgumentException in case of problems. */
  static <T extends Serializable> T readObject(File file, Class<T> expectedClass) {
    try {
      ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
      T result = expectedClass.cast(in.readObject());
      in.close();
      return result;
    }
    catch (IOException | ClassCastException | ClassNotFoundException excp) {
      throw new IllegalArgumentException(excp.getMessage());
    }
  }

  /*  Write the result of concatenating the bytes in CONTENTS to FILE,
   *  creating or overwriting it as needed.  Each object in CONTENTS may be
   *  either a String or a byte array.  Throws IllegalArgumentException
   *  in case of problems. */
  static void writeContents(File file, Object... contents) {
    try {
      if (file.isDirectory()) {
        throw new IllegalArgumentException("cannot overwrite directory");
      }
      BufferedOutputStream str = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
      for (Object obj : contents) {
        if (obj instanceof byte[]) {
          str.write((byte[]) obj);
        }
        else {
          str.write(((String) obj).getBytes(StandardCharsets.UTF_8));
        }
      }
      str.close();
    }
    catch (IOException | ClassCastException excp) {
      throw new IllegalArgumentException(excp.getMessage());
    }
  }

    /** Return a hashmap<Shift, int> representing the argument cso's schedule request
    *
    *   hashmap<Shift, int> where Shift corresponds to a specific Shift object,
    *   and int is their number preference for that shift.
    *   (NOTE: X's are treated as Integer.MAX_VALUE)
    *
    *   NOTE: the current implementation reads from CSV files, it will need to be
    *   changed if we change formats.
    *   The best website I could find to convert .xlsx to .csv is:
    *   https://www.convertsimple.com/convert-xlsx-to-csv/
    *   Every other option I found had a conversion limit and messed with naming conventions
    */
    static HashMap<Shift, Integer> scheduleRequest(CSO tempCSO) {

      int badge = tempCSO.getBadge();
      File scheduleRequestFile = new File(SCHEDULE_REQUEST_PATH ,String.format("#%d Schedule Request.csv", badge));
      if (!scheduleRequestFile.exists()) {
        scheduleRequestFile = new File(SCHEDULE_REQUEST_PATH ,String.format("#%d Schedule Request - Week 1.csv", badge));
        if (!scheduleRequestFile.exists()) {
          scheduleRequestFile = new File(SCHEDULE_REQUEST_PATH ,String.format("#%d Schedule Request - Week 2.csv", badge));
          if (!scheduleRequestFile.exists()) {
            System.out.println("Not able to find Schedule Request file.");
            tempCSO.setHoursReq(0);
            return new HashMap<Shift, Integer>();
          }
        }
      }

      //First build the list of Shift objects which will be the HashMap keys
      File[] listOfShifts = SHIFT_PATH.listFiles();
      List<Shift> listOfShiftObjects = new ArrayList<Shift>();
      for (File file : listOfShifts) {
        listOfShiftObjects.add(readObject(file, Shift.class));
      }

      //Then pull the data from the schedule request, we store the whole CSV as a 2D array of strings corresponding to excel cells
      //System.out.println("File path: " + String.format("#%d Schedule Request.csv", badge));
      List<List<String>> requestArray = new ArrayList<>();

      try (BufferedReader br = new BufferedReader(new FileReader(scheduleRequestFile))) {
        String line;
        while ((line = br.readLine()) != null) {
          String[] values = line.split(",");
          requestArray.add(Arrays.asList(values));
        }
      } catch (IOException | ClassCastException excp) {
        System.out.println("IOException " + excp.getMessage());
          //throw new IllegalArgumentException(excp.getMessage());
      }
      /*
      for (List<String> line : requestArray) {
        for (String cell : line) {
          System.out.print(cell + ", ");
        }
        System.out.println();
      }
      */

      //Now we have everything we need. First we'll update the CSO's requested hours.
      Double newCSOHours = readRequestDouble(requestArray, 17, 5, badge);
      if (newCSOHours == 0.0) {
        newCSOHours = 6.0 * readRequestDouble(requestArray, 17, 1, badge);
      }
      updateHoursRequested(tempCSO, newCSOHours);

      //Now we build the actual HashMap
      HashMap<Shift, Integer> map = new HashMap<Shift, Integer>();

      //for (List<String> row : requestArray) {
      for (int i = 4; i < 17; i++) {
        for (int j = 1; j < 8; j++) {
          List<Shift> shifts = parseShift(requestArray, i, j, listOfShiftObjects);
          int value = readRequestInt(requestArray, i, j, badge);                //Should we include Xs and blanks in the hashmap, or just shifts they can work?
          for (Shift shift : shifts) {
            if (value <= 10 && checkRank(tempCSO, shift)) {
              map.put(shift, value);
            }
          }
        }
      }

      /*
      this is here in case we ever need to save any changes in the shift files, isn't currently needed as we don't change them.
      for (Shift tempShift : listOfShiftObjects) {
        File shiftFile = new File(SHIFT_PATH, tempShift.getTitle());
        byte[] serializedShift = serialize(tempShift);
        writeContents(shiftFile, serializedShift);
      }
      */

      return map;
    }

    /*
        Helper function for scheduleRequest(). Returns true if cso's rank
        is sufficient to work shift.
    */
    static boolean checkRank(CSO cso, Shift shift) {
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

    static List<Shift> parseShift(List<List<String>> requestArray, int x, int y, List<Shift> listOfShiftObjects) {
      String dayOfWeek = "";
      switch (y) {
        case 1: dayOfWeek = "Sunday";
                break;
        case 2: dayOfWeek = "Monday";
                break;
        case 3: dayOfWeek = "Tuesday";
                break;
        case 4: dayOfWeek = "Wednesday";
                break;
        case 5: dayOfWeek = "Thursday";
                break;
        case 6: dayOfWeek = "Friday";
                break;
        case 7: dayOfWeek = "Saturday";
                break;
      }
      String title = "";
      Boolean ckcfh = false;
      switch (x) {
        case 4: title = "RSF AM";
                break;
        case 5: title = "RSF PM";
                break;
        case 6: title = "UHall";
                break;
        case 7: title = "BWW";
                break;
        case 9: title = "Bechtel";
                break;
        case 10: title = "Supervisor";
                break;
        case 11: title = "Dispatch";
                break;
        case 12: title = "Early Escort";
                break;
        case 13: title = "Blackwell";
                break;
        case 14: title = "Late Escort";
                break;
        case 15: title = "Units CSO";
        //case 15: title = "CKC";
        //        ckcfh = true;
        //        break;
        case 16: title = "Housing IC";
                break;
      }
      List<Shift> shifts = new ArrayList<Shift>();
      for (Shift shift : listOfShiftObjects) {
        if (shift.getTitle().equals(title) && shift.getDayOfWeek().equals(dayOfWeek)) {
          shifts.add(shift);
        }
      }
      if (ckcfh) {
        title = "FH";
        for (Shift shift : listOfShiftObjects) {
          if (shift.getTitle().equals(title) && shift.getDayOfWeek().equals(dayOfWeek)) {
            shifts.add(shift);
          }
        }
      }
      return shifts;
    }

    static int readRequestInt(List<List<String>> requestArray, int x, int y) {
      return readRequestInt(requestArray, x, y, 0);
    }

    static int readRequestInt(List<List<String>> requestArray, int x, int y, int badge) {
      if (y >= requestArray.get(x).size()) {
        return Integer.MAX_VALUE;
      }
      String input = requestArray.get(x).get(y);
      int result = Integer.MAX_VALUE;

      if (input == null || input == "" || input == "x" || input == "X") {
        return result;
      }

      try {
        result = Integer.parseInt(input);
      } catch (NumberFormatException nfe) {
        //System.out.println("Unable to parse the entry in field " + Integer.toString(x) + ", " + Integer.toString(x) + " of CSO " + Integer.toString(badge) + ". Entry was: " + input);
        Scanner sc = new Scanner(input);
        if (sc.hasNextInt()) {
          result = sc.nextInt();
          //System.out.println("Using first int found in string via Scanner: " + Integer.toString(result));
        }
      }
      return result;
    }

    static Double readRequestDouble(List<List<String>> requestArray, int x, int y) {
      return readRequestDouble(requestArray, x, y, 0);
    }

    static Double readRequestDouble(List<List<String>> requestArray, int x, int y, int badge) {
      if (y >= requestArray.get(x).size()) {
        return 0.0; //Changed from MAX_VALUE to 0.0
      }
      String input = requestArray.get(x).get(y);
      Double result = 0.0;

      // Not sure if this is needed since the case of a blank cell is handled above.
      /*
      if (input == null || input == "" || input == "x" || input == "X") {
        return result;
      }*/

      try {
        result = Double.parseDouble(input);
      } catch (NumberFormatException nfe) {
        //System.out.println("Unable to parse the entry in field " + Integer.toString(x) + ", " + Integer.toString(x) + " of CSO " + Integer.toString(badge) + ". Entry was: " + input);
        Scanner sc = new Scanner(input);
        if (sc.hasNextDouble()) {
          result = sc.nextDouble();
          //System.out.println("Using first int found in string via Scanner: " + Double.toString(result));
        } else {
          char[] inputChars = input.toCharArray();
          String temp = "";
          boolean currentlyInNum = false;
          for (char c : inputChars) {
            if (Character.isDigit(c)) {
              temp = temp + c;
              currentlyInNum = true;
            } else {
              if (currentlyInNum) {
                result = Double.parseDouble(temp);
                break;
              }
            }
          }
        }
      }
      System.out.println("parsed " + input + " as " + result);
      return result;
    }

    static void updateHoursRequested(CSO tempCSO, double hours) {
      tempCSO.setHoursReq(hours);
      return;
    }

    public static void writeToSpreadsheet(HashMap<Shift, CSO> oldSchedule) {

      //In this block we painstakingly cycle through the shifts in the schedule
      //to get the info we need. This ensures mutability as we get new Shifts
      //or the program loses shifts.
      ArrayList<String> daysOfTheWeek = new ArrayList<String>(Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"));
      ArrayList<String> shiftTitles = new ArrayList<String>();
      HashMap<String, Integer> shiftIters = new HashMap<String, Integer>();
      ArrayList<Shift> shifts = new ArrayList<Shift>(oldSchedule.keySet());
      Collections.sort(shifts);
      for (Shift shift : shifts) {
        String title = shift.getTitle();
        if (!shiftTitles.contains(title)) {
          shiftTitles.add(title);
        }
        if (shiftIters.get(title) == null) {
          shiftIters.put(title, shift.getIteration());
        } else if (shift.getIteration() > shiftIters.get(title)){
          shiftIters.put(title, (Integer)shift.getIteration());
        }
      }

      try (PrintWriter writer = new PrintWriter("Schedule.csv")) {
        HashMap<String, String> schedule = objectsToStrings(oldSchedule);
        StringBuilder sb = new StringBuilder();
        sb.append("Shift / Day");
        for (String day : daysOfTheWeek) {
          sb.append("," + day);
        }
        sb.append('\n');
        for (String title : shiftTitles) {
          for (int i = 1; i <= shiftIters.get(title); i++) {
            makeRow(schedule, title, sb, i);
          }
        }
        writer.write(sb.toString());
      } catch (FileNotFoundException e) {
        System.out.println(e.getMessage());
      }
    }

    private static HashMap<String, String> objectsToStrings(HashMap<Shift, CSO> schedule) {
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

    private static void makeRow(HashMap<String, String> schedule, String title, StringBuilder sb, int iteration) {
      List<String> daysOfTheWeek = Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
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
