package com.peanut.androidlib.sensormanager;
final class StateExceptionThrower {
    private static final String ALREADY_INITIAL = "State is initial already";
    private static final String ALREADY_STARTED = "Already started";
    private static final String ALREADY_STOPPED = "Already stopped";
    private static final String IS_NOT_PAUSING = "Is not pausing";
    private static final String NOT_STARTED = "Has not started";
    private static final String ALREADY_PAUSED = "Already paused";
    private State state;
    void validateInitial() {
        if (state == State.INITIAL) {
            throw new IllegalStateException(ALREADY_INITIAL);
        }
        state = State.INITIAL;
    }
    void validateStart() {
        if (state == State.START || state == State.RESUME || state == State.PAUSE) {
            throw new IllegalStateException(ALREADY_STARTED);
        }
        if (state == State.STOP) {
            throw new IllegalStateException(ALREADY_STOPPED);
        }
        state = State.START;
    }
    void validateResume() {
        if (state != State.PAUSE) {
            throw new IllegalStateException(IS_NOT_PAUSING);
        }
        state = State.RESUME;
    }
    void validatePause() {
        if (state == State.INITIAL) {
            throw new IllegalStateException(NOT_STARTED);
        }
        if (state == State.PAUSE) {
            throw new IllegalStateException(ALREADY_PAUSED);
        }
        if (state == State.STOP) {
            throw new IllegalStateException(ALREADY_STOPPED);
        }
        state = State.PAUSE;
    }
    void validateStop() {
        if (state == State.INITIAL) {
            throw new IllegalStateException(NOT_STARTED);
        }
        if (state == State.STOP) {
            throw new IllegalStateException(ALREADY_STOPPED);
        }
        state = State.STOP;
    }
    State getState() {
        return state;
    }
    enum State {
        INITIAL, START, RESUME, PAUSE, STOP
    }
}
