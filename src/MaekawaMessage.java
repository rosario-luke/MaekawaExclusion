import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Lucas Rosario on 4/16/2015.
 */
public class MaekawaMessage {

    static final int RELEASE = 0;
    static final int REQUEST = 1;
    static final int INIT = 100;
    static final int REQUEST_REPLY = 2;
    static final int FAIL = 3;
    static final int YIELD = 4;
    static final int INQUIRE = 5;


    private int origin_id;
    private LinkedBlockingQueue<MaekawaMessage> return_queue;
    private int type;
    private long timestamp;
    private boolean hasSentFail;
    private boolean hasSentInquire;

    public MaekawaMessage(int id, LinkedBlockingQueue<MaekawaMessage> q, int t, int s){
        origin_id = id;
        return_queue = q;
        type = t;
        //timestamp = System.currentTimeMillis() + r*10;
        timestamp = s + id;
        hasSentFail = false;
        hasSentInquire = false;
    }

    public int getOrigin(){ return origin_id;}

    public void sentFail(){
        hasSentFail = true;
    }

    public LinkedBlockingQueue<MaekawaMessage> getReturnQueue(){ return return_queue;}

    public boolean isRelease(){ return type == RELEASE;}

    public boolean isRequest(){ return type == REQUEST;}

    public long getTimestamp(){ return timestamp;}

    public boolean isFail(){ return type == FAIL;}

    public boolean hasSentFail(){ return hasSentFail;}

    public boolean hasSentInquire(){ return hasSentInquire;}

    public void sentInquire(){ hasSentInquire = true;}

    public boolean isYield(){ return type == YIELD;}

    public boolean isInit(){ return type == INIT;}

    public boolean isInquire(){ return type == INQUIRE;}

    public boolean isRequestReply(){ return type == REQUEST_REPLY;}

    public String getTypeString(){
        switch(type) {
            case RELEASE:
                return "RELEASE";
            case REQUEST:
                return "REQUEST";
            case REQUEST_REPLY:
                return "REQUEST_REPLY";
            case YIELD:
                return "YIELD";
            case FAIL:
                return "FAIL";
            case INQUIRE:
                return "INQUIRE";
            default:
                return "MESSAGE";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaekawaMessage)) return false;

        MaekawaMessage that = (MaekawaMessage) o;

        if (origin_id != that.origin_id) return false;
        if (timestamp != that.timestamp) return false;
        if (type != that.type) return false;
        if (!return_queue.equals(that.return_queue)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = origin_id;
        result = 31 * result + return_queue.hashCode();
        result = 31 * result + type;
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        return result;
    }
}
