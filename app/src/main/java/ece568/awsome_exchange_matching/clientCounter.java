package ece568.awsome_exchange_matching;

public class clientCounter {
    /**
     * a singleton thread-safe client counter
     * start from 0
     */
    private static clientCounter counter_obj = null;
    private static int next_counter;
    private static int current_id;
    private clientCounter(){
        next_counter = 0;
    }
    public static clientCounter getInstance(){
        if (counter_obj == null){
            synchronized(clientCounter.class){
                if (counter_obj == null){
                    counter_obj = new clientCounter();
                }
            }
        }
        current_id = next_counter;
        next_counter++;
        return counter_obj;
    }

    public static int getCurrent_id() {
        return current_id;
    }

}
