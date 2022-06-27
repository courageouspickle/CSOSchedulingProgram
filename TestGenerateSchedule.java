import java.util.*;


public class TestGenerateSchedule {

    public static void main(String[] args) {

        testScheduleRequestBasic();
     }

    public static void testScheduleRequestBasic() {
        System.out.println("197's request: (should be full)");
        CSO cso197 = new CSO(197);
        HashMap<Shift, Integer> hm197 = generateSchedule.scheduleRequest(cso197);
        System.out.println(hm197);
        System.out.println("\n152's request: (should be empty)");
        CSO cso152 = new CSO(152);
        HashMap<Shift, Integer> hm152 = generateSchedule.scheduleRequest(cso152);
        System.out.println(hm152);
        return;
    }
}
