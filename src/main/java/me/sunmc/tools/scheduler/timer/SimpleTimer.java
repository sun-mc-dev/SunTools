package me.sunmc.tools.scheduler.timer;

import me.sunmc.tools.Tools;
import me.sunmc.tools.scheduler.interfaces.SchedulerAdapter;
import me.sunmc.tools.scheduler.interfaces.SchedulerTask;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Enhanced timer with advanced features:
 * - Event callbacks
 * - Pause/resume capability
 * - Custom intervals
 * - Multiple tick listeners
 * - Auto-restart option
 *
 * @version 1.0.0
 */
public class SimpleTimer {

    /**
     * Default interval time in seconds.
     */
    public static final long DEFAULT_INTERVAL_TIME = 1;

    /**
     * Default {@link TimeUnit} for the interval time.
     */
    public static final @NonNull TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    private final @NonNull String identifier;
    private final int startingValue;
    private final int stopValue;
    private final @NonNull List<Consumer<Integer>> tickCallbacks;
    private final @NonNull List<Runnable> completeCallbacks;

    private int timer;
    private @Nullable SchedulerTask task;
    private boolean paused = false;
    private boolean autoRestart = false;
    private long intervalTime = DEFAULT_INTERVAL_TIME;
    private @NonNull TimeUnit timeUnit = DEFAULT_TIME_UNIT;

    /**
     * Creates a new timer with a specified identifier and starting value.
     *
     * @param identifier    Unique identifier for the timer.
     * @param startingValue Initial value of the timer.
     */
    public SimpleTimer(@NonNull String identifier, int startingValue) {
        this(identifier, startingValue, -1);
    }

    /**
     * Constructs a timer with a specified identifier, starting value, and stop value.
     *
     * @param identifier    Unique identifier for the timer.
     * @param startingValue Initial value of the timer.
     * @param stopValue     Value at which the timer should stop, or -1 if there's no stop value.
     */
    public SimpleTimer(@NonNull String identifier, int startingValue, int stopValue) {
        this.identifier = identifier;
        this.startingValue = startingValue;
        this.timer = startingValue;
        this.stopValue = stopValue;
        this.tickCallbacks = new ArrayList<>();
        this.completeCallbacks = new ArrayList<>();
    }

    /**
     * Starts the timer with the specified change type.
     *
     * @param change The type of change to perform on the timer.
     * @return The {@link SchedulerTask} associated with the timer.
     */
    public @NonNull SchedulerTask startTimer(TimeChange change) {
        return this.startTimer(change, false);
    }

    /**
     * Starts the timer with the specified change type and execution mode.
     *
     * @param change The type of change to perform on the timer.
     * @param async  Indicates whether the timer task should run asynchronously.
     * @return The {@link SchedulerTask} associated with the timer.
     */
    public @NonNull SchedulerTask startTimer(TimeChange change, boolean async) {
        if (this.task != null) {
            throw new IllegalStateException("There is already an active task running in " + this.getClass().getSimpleName());
        }

        final Runnable task = () -> {
            if (this.paused) {
                return;
            }

            int current;

            // Increment or decrement timer
            if (change == TimeChange.INCREMENT) {
                current = this.increment();
            } else {
                current = this.decrement();
            }

            // Notify tick callbacks
            this.tickCallbacks.forEach(callback -> callback.accept(current));

            // Stop the timer if reached stop value
            if (this.stopValue != -1 && current == this.stopValue) {
                this.stopTimer();
                this.completeCallbacks.forEach(Runnable::run);

                if (this.autoRestart) {
                    this.reset();
                    this.startTimer(change, async);
                }
            }
        };

        final SchedulerAdapter adapter = Tools.getInstance().getSchedulerAdapter();
        if (async) {
            return this.task = adapter.asyncRepeating(task, this.intervalTime, this.intervalTime, this.timeUnit);
        } else {
            return this.task = adapter.syncRepeating(task, this.intervalTime, this.intervalTime, this.timeUnit);
        }
    }

    /**
     * Stops the timer task associated with this timer.
     */
    public void stopTimer() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    /**
     * Pauses the timer (stops updating but doesn't cancel the task).
     */
    public void pause() {
        this.paused = true;
    }

    /**
     * Resumes the timer if paused.
     */
    public void resume() {
        this.paused = false;
    }

    /**
     * Toggles pause state.
     */
    public void togglePause() {
        this.paused = !this.paused;
    }

    /**
     * Checks if the timer is paused.
     *
     * @return True if paused, false otherwise.
     */
    public boolean isPaused() {
        return this.paused;
    }

    /**
     * Checks if the timer is running.
     *
     * @return True if running, false otherwise.
     */
    public boolean isRunning() {
        return this.task != null && !this.paused;
    }

    /**
     * Adds a callback to be called on each tick.
     *
     * @param callback The callback accepting current timer value.
     * @return This timer instance for chaining.
     */
    public @NonNull SimpleTimer onTick(@NonNull Consumer<Integer> callback) {
        this.tickCallbacks.add(callback);
        return this;
    }

    /**
     * Adds a callback to be called when timer completes.
     *
     * @param callback The callback to execute on completion.
     * @return This timer instance for chaining.
     */
    public @NonNull SimpleTimer onComplete(@NonNull Runnable callback) {
        this.completeCallbacks.add(callback);
        return this;
    }

    /**
     * Sets whether the timer should auto-restart after completion.
     *
     * @param autoRestart True to auto-restart, false otherwise.
     * @return This timer instance for chaining.
     */
    public @NonNull SimpleTimer setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
        return this;
    }

    /**
     * Sets the interval time for the timer.
     *
     * @param intervalTime The interval time.
     * @param timeUnit The time unit.
     * @return This timer instance for chaining.
     */
    public @NonNull SimpleTimer setInterval(long intervalTime, @NonNull TimeUnit timeUnit) {
        this.intervalTime = intervalTime;
        this.timeUnit = timeUnit;
        return this;
    }

    /**
     * Gets the unique identifier of this timer.
     *
     * @return The unique identifier of the timer.
     */
    public @NonNull String getIdentifier() {
        return this.identifier;
    }

    /**
     * Force sets the timer value at a specific value.
     *
     * @param value The value to set the timer at.
     */
    public void set(int value) {
        this.timer = value;
    }

    /**
     * Increments the timer by 1.
     *
     * @return The updated value of the timer after incrementing.
     */
    public int increment() {
        return ++this.timer;
    }

    /**
     * Increments the timer by the provided value.
     *
     * @param value The value to increment the timer by.
     * @return The updated value of the timer after incrementing.
     */
    public int incrementBy(int value) {
        return this.timer += value;
    }

    /**
     * Decrements the timer by 1.
     *
     * @return The updated value of the timer after decrementing.
     */
    public int decrement() {
        return --this.timer;
    }

    /**
     * Decrements the timer by the provided value.
     *
     * @param value The value to decrement the timer by.
     * @return The updated value of the timer after decrementing.
     */
    public int decrementBy(int value) {
        return this.timer -= value;
    }

    /**
     * Resets the timer to its starting value.
     *
     * @return The starting value of the timer after resetting.
     */
    public int reset() {
        return this.timer = this.startingValue;
    }

    /**
     * Gets the current value of the timer.
     *
     * @return The current value of the timer.
     */
    public int get() {
        return this.timer;
    }

    /**
     * Gets the starting value of the timer.
     *
     * @return The starting timer value.
     */
    public int getStartingValue() {
        return this.startingValue;
    }

    /**
     * Gets the stop value of the timer.
     *
     * @return The stop timer value (-1 if no stop value).
     */
    public int getStopValue() {
        return this.stopValue;
    }

    /**
     * Gets the remaining time until stop value.
     *
     * @return Remaining count, or -1 if no stop value.
     */
    public int getRemaining() {
        if (this.stopValue == -1) {
            return -1;
        }
        return Math.abs(this.timer - this.stopValue);
    }

    /**
     * Gets the progress as a percentage (0.0 to 1.0).
     *
     * @return Progress percentage, or 0.0 if no stop value.
     */
    public double getProgress() {
        if (this.stopValue == -1) {
            return 0.0;
        }

        int total = Math.abs(this.stopValue - this.startingValue);
        int current = Math.abs(this.timer - this.startingValue);
        return total == 0 ? 0.0 : (double) current / total;
    }

    /**
     * Enum representing the types of changes that can be applied to the timer.
     */
    public enum TimeChange {
        /**
         * Increment the timer value.
         */
        INCREMENT,
        /**
         * Decrement the timer value.
         */
        DECREMENT
    }
}