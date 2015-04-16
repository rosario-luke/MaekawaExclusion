import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Lucas Rosario on 4/16/2015.
 */
public class MaekawaMessage {

    static final int RELEASE = 0;
    static final int REQUEST = 1;
    static final int INIT = 100;
    static final int REQUEST_REPLY = 2;


    private int origin_id;
    private LinkedBlockingQueue<MaekawaMessage> return_queue;
    private int type;
    private long timestamp;

    public MaekawaMessage(int id, LinkedBlockingQueue<MaekawaMessage> q, int t){
        origin_id = id;
        return_queue = q;
        type = t;
        timestamp = System.currentTimeMillis();
    }

    public int getOrigin(){ return origin_id;}

    public LinkedBlockingQueue<MaekawaMessage> getReturnQueue(){ return return_queue;}

    public boolean isRelease(){ return type == RELEASE;}

    public boolean isRequest(){ return type == REQUEST;}

    public long getTimestamp(){ return timestamp;}

    public boolean isInit(){ return type == INIT;}

    public boolean isRequestReply(){ return type == REQUEST_REPLY;}

    public String getTypeString(){
        switch(type) {
            case RELEASE:
                return "RELEASE";
            case REQUEST:
                return "REQUEST";
            case REQUEST_REPLY:
                return "REQUEST_REPLY";
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
