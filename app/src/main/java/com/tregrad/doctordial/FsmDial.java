package com.tregrad.doctordial;

import android.util.Log;

/**
 * Created by piduck on 08/11/16.
 */
public class FsmDial {
    private static FsmDial instance = new FsmDial();
    public static FsmDial getInstance() {
        assert instance != null;
        return instance;
    }

    public enum State {
        IDLE,
        ACTIVE,
        HALTING
    }
    private State state;

    private FsmDial() {state = State.IDLE; }
    public State getState() { return state; }
    public void activate() {
        state = State.ACTIVE;
    }
    public void halt() {
        //state = State.IDLE;
        if(state == State.ACTIVE) state = State.HALTING;
        else Log.d("FSMDial", "Trying to halt when not active");
        //TODO: remove
        reset();
    }
    public void reset() {
        if(state == State.HALTING) state = State.IDLE;
        else Log.d("FSMDial", "Trying to reset halting state when not halting");
    }
}
