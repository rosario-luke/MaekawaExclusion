import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Lucas Rosario on 4/16/2015.
 */
public class CoordinatorThread {


    final static int[][] node_id_array = new int[][]{
            {1,2,3}, {4,5,6}, {7,8,9}
    };

    public static void main(String[] args){

        if(!(args.length == 4)){
            System.out.println("Incorrect Arguments:  <cs_int> <next_req> <tot_exec_time> <option>");
            System.exit(1);
        }

        MaekawaOptions config = null;

        try {
            config = new MaekawaOptions(args);
        } catch(Exception e){
            e.printStackTrace();
            System.out.println("Error occurred while parsing arguments. INVALID ARGUMENTS");
            System.exit(1);
        }

        LinkedBlockingQueue[] queues = new LinkedBlockingQueue[9];
        Object init_condition = new Object();
        for(int i = 0; i < 9; i++){
            queues[i] = new LinkedBlockingQueue<MaekawaMessage>(200);
        }
        Integer CSSTAT = 0;
        ArrayList<MaekawaThread> myThreads = createThreads(queues, config, init_condition, CSSTAT);

        System.out.println("Created Threads -- Going to start");

        for(int i = 0; i<myThreads.size(); i++){
            myThreads.get(i).start();
        }

        sleep(500);


        int c = 0;
        while(c < 5) {
            synchronized (init_condition) {
                c++;
                init_condition.notifyAll();
            }

        }




        Timer timer = new Timer();
        timer.schedule(new MyExitTimer(myThreads, config.getTotExecTime()), new Long(config.getTotExecTime()*1000));



        for(int i =0; i<myThreads.size(); i++){
            try {
                myThreads.get(i).getThread().join();
            } catch(InterruptedException e){
                e.printStackTrace();
            }
        }










    }

    public static void sleep(long mil){
        boolean errorOccurred = true;
        while(errorOccurred) {
            try {
                Thread.sleep(mil);
                errorOccurred = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static ArrayList<MaekawaThread> createThreads(LinkedBlockingQueue<MaekawaMessage>[] queues, MaekawaOptions config, Object init_condition, Integer CSSTAT){
        ArrayList<MaekawaThread> threads = new ArrayList<MaekawaThread>();

        for(int i = 1; i < 10; i++){

            ArrayList<Integer> vInts = getVotingSet(i);
            ArrayList<LinkedBlockingQueue<MaekawaMessage>> vSet = new ArrayList<LinkedBlockingQueue<MaekawaMessage>>();
            System.out.print(i + " :");
            for(Integer pos : vInts){
                vSet.add(queues[pos-1]);
                System.out.print(" " + pos + " ");
            }

            threads.add(new MaekawaThread(i, queues[i-1], vSet, vInts, config, init_condition, CSSTAT));
            System.out.println("");
        }

        return threads;
    }


    public static ArrayList<Integer> getVotingSet(int identifier){
        ArrayList<Integer> vSet = new ArrayList<Integer>();

        int row = 0;
        int col = 0;
        for(int i = 0; i < 3; i++){
            for(int j = 0; j<3; j++){
                if(node_id_array[i][j] == identifier){
                    row = i;
                    col = j;
                    break;
                }
            }
        }

        for(int i = 0; i < 3; i++){
            if(node_id_array[row][i] != identifier){ vSet.add(node_id_array[row][i]); }
            vSet.add(node_id_array[i][col]);
        }

        return vSet;


    }

    public static class MyExitTimer extends TimerTask{

        ArrayList<MaekawaThread> myThreads;
        int wait_time;

        public MyExitTimer(ArrayList<MaekawaThread> mT, int wT){
            myThreads = mT;
            wait_time = wT;
        }

        public void run(){
            for(int i = 0; i < myThreads.size(); i++){
                myThreads.get(i).stop();
            }

            System.out.println("Waited for " + wait_time + " seconds -- Exiting");

            System.exit(0);
        }

    }


}
