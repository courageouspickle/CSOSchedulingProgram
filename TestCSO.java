import java.util.*;


public class TestCSO {
  public static void main(String[] args) {
    testRank();
  }

  public static void testRank() {
    CSO cso = new CSO(197);
    System.out.println("These should all be \"PC\"");
    cso.setRank("Program Coordinator");
    System.out.println(cso.getRank());
    cso.setRank("P");
    System.out.println(cso.getRank());
    cso.setRank("Pc");
    System.out.println(cso.getRank());
    cso.setRank("4");
    System.out.println(cso.getRank());
    System.out.println("These should all be \"Supervisor\"");
    cso.setRank("SuPeRvIsOr");
    System.out.println(cso.getRank());
    cso.setRank("supe");
    System.out.println(cso.getRank());
    cso.setRank("s");
    System.out.println(cso.getRank());
    cso.setRank("3");
    System.out.println(cso.getRank());
    System.out.println("These should all be \"General\"");
    cso.setRank("general");
    System.out.println(cso.getRank());
    cso.setRank("g");
    System.out.println(cso.getRank());
    cso.setRank("general CSO");
    System.out.println(cso.getRank());
    cso.setRank("1");
    System.out.println(cso.getRank());
    System.out.println("These should all be \"Trainee\"");
    cso.setRank("t");
    System.out.println(cso.getRank());
    cso.setRank("0");
    System.out.println(cso.getRank());
    cso.setRank("trainee");
    System.out.println(cso.getRank());
    cso.setRank("FTE");
    System.out.println(cso.getRank());

    //System.out.println("this should throw an error, if you aren't trying to test, comment out this test in TestCSO.java");
    //cso.setRank("supreme Overlord of CSOs");
  }
}
