package cse311;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages tasks (processes) in the simulated operating system.
 * Provides functionality for task creation, scheduling, and context switching.
 */
public class TaskManager {
    private List<Task> tasks;
    private int currentTaskIndex;
    private RV32iCpu cpu;
    private MemoryManager memory;
    private static final int DEFAULT_STACK_SIZE = 4096; // 4KB stack per task
    
    /**
     * Creates a new TaskManager.
     * 
     * @param cpu The CPU to manage tasks for
     * @param memory The memory manager
     */
    public TaskManager(RV32iCpu cpu, MemoryManager memory) {
        this.tasks = new ArrayList<>();
        this.currentTaskIndex = -1; // No active task initially
        this.cpu = cpu;
        this.memory = memory;
    }
    
    /**
     * Creates a new task with the specified entry point.
     * 
     * @param entryPoint The entry point (initial PC value)
     * @return The ID of the created task
     * @throws MemoryAccessException If there is not enough memory for the task's stack
     */
    public int createTask(int entryPoint) throws MemoryAccessException {
        int taskId = tasks.size();
        
        // Allocate stack space for the task
        // Each task gets its own stack region
        int stackBase = memory.getStackPointer() - DEFAULT_STACK_SIZE;
        if (stackBase <= memory.getHeapPointer()) {
            throw new MemoryAccessException("Not enough memory for task stack");
        }
        
        // Create the task
        Task task = new Task(taskId, entryPoint, DEFAULT_STACK_SIZE, stackBase);
        tasks.add(task);
        
        // If this is the first task, make it active
        if (tasks.size() == 1) {
            currentTaskIndex = 0;
            task.setActive(true);
            task.restoreState(cpu);
        }
        
        return taskId;
    }
    
    /**
     * Switches to the next task in the round-robin schedule.
     * This implements cooperative multitasking via the yield syscall.
     */
    public void yield() {
        if (tasks.isEmpty()) {
            return; // No tasks to switch to
        }
        
        // Save current task state
        if (currentTaskIndex >= 0 && currentTaskIndex < tasks.size()) {
            Task currentTask = tasks.get(currentTaskIndex);
            currentTask.saveState(cpu);
            currentTask.setActive(false);
        }
        
        // Move to next task (round-robin)
        currentTaskIndex = (currentTaskIndex + 1) % tasks.size();
        
        // Restore next task state
        Task nextTask = tasks.get(currentTaskIndex);
        nextTask.setActive(true);
        nextTask.restoreState(cpu);
    }
    
    /**
     * Gets the currently active task.
     * 
     * @return The currently active task, or null if no task is active
     */
    public Task getCurrentTask() {
        if (currentTaskIndex >= 0 && currentTaskIndex < tasks.size()) {
            return tasks.get(currentTaskIndex);
        }
        return null;
    }
    
    /**
     * Gets a task by its ID.
     * 
     * @param taskId The task ID
     * @return The task, or null if no task with the specified ID exists
     */
    public Task getTask(int taskId) {
        if (taskId >= 0 && taskId < tasks.size()) {
            return tasks.get(taskId);
        }
        return null;
    }
    
    /**
     * Gets the number of tasks.
     * 
     * @return The number of tasks
     */
    public int getTaskCount() {
        return tasks.size();
    }
}