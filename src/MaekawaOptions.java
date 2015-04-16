/**
 * Created by Lucas Rosario on 4/16/2015.
 */
public class MaekawaOptions {



    private int cs_int, next_req, tot_exec_time;
    private boolean printOption;

    public MaekawaOptions(int csInt, int nR, int tE, boolean p){
        cs_int = csInt;
        next_req = nR;
        tot_exec_time = tE;
        printOption = p;
    }

    public MaekawaOptions(String[] args) throws Exception{
        cs_int = Integer.parseInt(args[0]);
        next_req = Integer.parseInt(args[1]);
        tot_exec_time = Integer.parseInt(args[2]);
        printOption = Integer.parseInt(args[3]) == 1 ? true : false;
    }

    public int getCsInt() {
        return cs_int;
    }

    public int getNextReq() {
        return next_req;
    }

    public int getTotExecTime() {
        return tot_exec_time;
    }

    public boolean isPrintOption() {
        return printOption;
    }




}
