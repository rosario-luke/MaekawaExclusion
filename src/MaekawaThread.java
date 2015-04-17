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

    private Queue<MaekawaMessage> _requestQueue;

    public MaekawaThread(int id, LinkedBlockingQueue<MaekawaMessage> q, ArrayList<LinkedBlockingQueue<MaekawaMessage>> vSet, ArrayList<Integer> vSetId, MaekawaOptions c, Object cv){

        _votingSet = vSet;
        _identifier = id;
        _status = INIT;
        _voted = false;
        _myQueue = q;
        _votingSetIds = vSetId;
        _requestQueue = new LinkedList<MaekawaMessage>();
        config = c;
        _init_cv = cv;



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
            MaekawaMessage request = new MaekawaMessage(_identifier, _myQueue, MaekawaMessage.REQUEST);
            multicast(request);

            int num_replies = 0;

            // Wait until number of replies is met
            while (num_replies < _votingSet.size()) {

                MaekawaMessage received = receiveMessage();
                if(received.isRequest()){
                    if(_voted){
                        _requestQueue.add(received);
                        continue;
                    } else {
                        MaekawaMessage reply = new MaekawaMessage(_identifier, _myQueue, MaekawaMessage.REQUEST_REPLY);
                        reply(reply, received);
                        _voted = true;
                        continue;
                    }
                } else if(received.isRequestReply()){
                    num_replies++;
                    continue;
                } else if(received.isRelease()){
                    if(!_requestQueue.isEmpty()) {
                        MaekawaMessage prev_request = _requestQueue.remove();
                        MaekawaMessage reply = new MaekawaMessage(_identifier, _myQueue, MaekawaMessage.REQUEST_REPLY);
                        reply(reply, prev_request);
                        _voted = true;
                        continue;
                    } else {
                        _voted = false;
                        continue;
                    }
                }
            }

            _status = HELD;

            criticalSection();

            _status = RELEASE;

            MaekawaMessage release = new MaekawaMessage(_identifier, _myQueue, MaekawaMessage.RELEASE);
            multicast(release);

            sleep(config.getNextReq());

            _status = REQUEST;


        }


    }

    public Thread getThread(){
        return _t;
    }

    public void criticalSection(){

        System.out.print("time(" + System.currentTimeMillis() + ") id(" + _identifier + ") node-list(");
        for(Integer i : _votingSetIds){
            System.out.print(i + " ");
        }
        System.out.print(")");
        System.out.println("");
        sleep(config.getCsInt());
    }

    public void sleep(int secs){
        long mil = secs*1000;
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
                        System.out.println(System.currentTimeMillis() + " " + _identifier + " " + received.getOrigin() + " " + received.getTypeString());
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
                    received.getReturnQueue().put(toSend);
                    return;
                } catch (Exception e) {
                }
            }
        }
    }


}

