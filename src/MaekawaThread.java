import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Lucas Rosario on 4/16/2015.
 */
public class MaekawaThread implements Runnable {

    private static final int RELEASE = 0;
    private static final int REQUEST = 1;
    private static final int HELD = 2;
    private static final int INIT = 100;


    private ArrayList<LinkedBlockingQueue<MaekawaMessage>> _votingSet;
    private ArrayList<Integer> _votingSetIds;
    private int _identifier;
    private int _status;
    private boolean _voted;
    private LinkedBlockingQueue<MaekawaMessage> _myQueue;
    private Thread _t;
    private MaekawaOptions config;
    private Object _init_cv;
    private MaekawaMessage _lastRepliedMessage;
    private Queue<MaekawaMessage> _requestQueue;
    private Queue<MaekawaMessage> _failQueue;
    private Integer CSSTAT;
    private int num_replies;
    private boolean have_inquired;
    private Random rand;
    private int num_success;

    public MaekawaThread(int id, LinkedBlockingQueue<MaekawaMessage> q, ArrayList<LinkedBlockingQueue<MaekawaMessage>> vSet, ArrayList<Integer> vSetId, MaekawaOptions c, Object cv, Integer cs){

        _votingSet = vSet;
        _identifier = id;
        _status = INIT;
        _voted = false;
        _myQueue = q;
        _votingSetIds = vSetId;
        //_requestQueue = new LinkedList<MaekawaMessage>();
        //_failQueue = new LinkedList<MaekawaMessage>();
        config = c;
        _init_cv = cv;
        _lastRepliedMessage = null;
        num_replies = 0;
        CSSTAT = cs;
        _requestQueue = new PriorityQueue<MaekawaMessage>(new MessageComparator());
        have_inquired = false;
        rand = new Random(System.currentTimeMillis());
        num_success = 0;
       /* for(int i = 0;i < 10; i++){
            MaekawaMessage m = new MaekawaMessage(i, null, 1);
            try{
                Thread.sleep(100);
            } catch(Exception e){

            }
            _requestQueue.add(m);
        }
        for(int i =0; i<10; i++){
            MaekawaMessage m = _requestQueue.poll();
            System.out.println(m.getTimestamp());
        }*/

    }

    public static class MessageComparator implements Comparator<MaekawaMessage>{
        public int compare(MaekawaMessage a, MaekawaMessage b){
            if(a.getTimestamp() < b.getTimestamp()){
                return -1;
            } else if(a.getTimestamp() == b.getTimestamp()){
                return 0;
            } else {
                return 1;
            }
        }
    }

    public void stop(){
        _t.interrupt();
    }

    public void start() {

        if (_t == null) {
            _t = new Thread(this, "Node:" + _identifier);
        }
        _t.start();
    }

    public void run(){
        System.out.println("Node(" + _identifier + ") has started");

        init();


        while(true){

            // REQUEST STAGE
            MaekawaMessage request = new MaekawaMessage(_identifier, _myQueue, MaekawaMessage.REQUEST, num_success);
            multicast(request);

            num_replies = 0;
            //_voted = false;

            // Wait until number of replies is met
            while (num_replies < _votingSet.size()) {

                MaekawaMessage received = receiveMessage();
                if(received.isRequest()){
                    /*if(_voted){
                        _requestQueue.add(received);
                        continue;
                    } else {
                        MaekawaMessage reply = new MaekawaMessage(_identifier, _myQueue, MaekawaMessage.REQUEST_REPLY);
                        reply(reply, received);
                        _lastRepliedMessage = received;
                        _voted = true;
                        continue;
                    }*/
                    request(received);
                } else if(received.isRequestReply()){
                    num_replies++;
                    continue;
                } else if(received.isRelease()){
                    //_lastRepliedMessage = null;
                    have_inquired = false;
                    if(!_requestQueue.isEmpty()) {
                        MaekawaMessage prev_request = _requestQueue.poll();
                        MaekawaMessage reply = new MaekawaMessage(_identifier, _myQueue, MaekawaMessage.REQUEST_REPLY, num_success);
                        reply(reply, prev_request);
                        _lastRepliedMessage = prev_request;
                        _voted = true;
                        continue;
                    } else {
                        _voted = false;
                        continue;
                    }
                } else if(received.isFail()){
                    _failQueue.add(received);

                } else if(received.isInquire()){

                        num_replies --;
                        MaekawaMessage yield = new MaekawaMessage(_identifier, _myQueue, MaekawaMessage.YIELD, num_success);
                        reply(yield, received);

                } else if(received.isYield()){
                    //_lastRepliedMessage = null;
                    //_voted = false;
                    _requestQueue.offer(_lastRepliedMessage);
                    MaekawaMessage prev_request = _requestQueue.poll();
                    MaekawaMessage reply = new MaekawaMessage(_identifier, _myQueue, MaekawaMessage.REQUEST_REPLY, num_success);
                    reply(reply, prev_request);
                    _lastRepliedMessage = prev_request;
                    _voted = true;
                    have_inquired = false;
                    continue;

                }
            }

            //_lastRepliedMessage = null;
            //_failQueue.clear();

            _status = HELD;

            criticalSection();

            _status = RELEASE;

            MaekawaMessage release = new MaekawaMessage(_identifier, _myQueue, MaekawaMessage.RELEASE, num_success);
            multicast(release);

            sleep(config.getNextReq());

            _status = REQUEST;




        }


    }

    public void request(MaekawaMessage r){
        if(_voted){
            _requestQueue.offer(r);
            if(_lastRepliedMessage.getTimestamp() == r.getTimestamp()){System.out.println("SAME TIMESTAMP");}
            if(_lastRepliedMessage.getTimestamp() > r.getTimestamp() && !have_inquired ){
                MaekawaMessage inquire = new MaekawaMessage(_identifier, _myQueue, MaekawaMessage.INQUIRE, num_success);
                reply(inquire, _lastRepliedMessage);

                have_inquired = true;
            }
        } else {
            MaekawaMessage reply = new MaekawaMessage(_identifier, _myQueue, MaekawaMessage.REQUEST_REPLY, num_success);
            reply(reply, r);
            _voted = true;
            _lastRepliedMessage = r;
        }
    }

    public Thread getThread(){
        return _t;
    }



    public void criticalSection(){
        num_success+=10;
        System.out.print("time(" + System.currentTimeMillis() + ") id(" + _identifier + ") node-list(");
        for(Integer i : _votingSetIds){
            System.out.print(i + " ");
        }
        System.out.print(")");
        System.out.println("");
        sleep(config.getCsInt());
    }

    public void sleep(int secs){
        long mil = secs;
        try{
            Thread.sleep(mil);
        } catch(Exception e){

        }
    }

    public MaekawaMessage receiveMessage(){
        boolean errorOccurred = true;
        MaekawaMessage received = null;
            while (errorOccurred) {
                try {
                    received = _myQueue.poll(5, TimeUnit.SECONDS);
                    if (received == null) {
                        continue;
                    }
                    _myQueue.remove(received);
                    while (_myQueue.contains(received)) {
                        _myQueue.remove(received);
                    }
                    if (config.isPrintOption()) {
                        System.out.println(System.currentTimeMillis() + " " + _identifier + " " + received.getOrigin() + " " + received.getTypeString() + " timestamp(" + received.getTimestamp() + ")");
                    }
                    return received;
                } catch (Exception e) {
                    errorOccurred = true;
                }
            }

        return null;
    }

    /**
     * Waits on the Condition Variable that signifies that all processes are running
     */
    public void init(){
        while(_status == INIT) {
            synchronized (_init_cv){
                try{

                    _init_cv.wait();
                    _init_cv.notifyAll();
                    _status = REQUEST;
                } catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void multicast(MaekawaMessage message){
        for(int i = 0; i < _votingSet.size(); i++){

            boolean errorOccurred = true;
            synchronized (_votingSet.get(0)) {
                while (errorOccurred) {
                    try {
                        _votingSet.get(i).put(message);
                        break;
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                        errorOccurred = true;
                    }
                }
            }
        }

    }

    public void reply(MaekawaMessage toSend, MaekawaMessage received){
        boolean errorOccurred = true;
        while(true) {
            synchronized (received.getReturnQueue()) {
                try {
                    received.getReturnQueue().offer(toSend);
                    return;
                } catch (Exception e) {
                }
            }
        }
    }


}

