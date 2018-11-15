package skynet.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.*;

public class Profiler extends AbstractActor {

	////////////////////////
	// Actor Construction //
	////////////////////////
	
	public static final String DEFAULT_NAME = "profiler";

	public static Props props() {
		return Props.create(Profiler.class);
	}

	////////////////////
	// Actor Messages //
	////////////////////
	
	@Data @AllArgsConstructor
	public static class RegistrationMessage implements Serializable {
		private static final long serialVersionUID = 4545299661052078209L;
	}

	@Data @AllArgsConstructor @SuppressWarnings("unused")
	public static class TaskMessage implements Serializable {
		private static final long serialVersionUID = -8330958742629706627L;
		private TaskMessage() {}
		private int attributes;
	}
	
	@Data @AllArgsConstructor @SuppressWarnings("unused")
	public static class CompletionMessage implements Serializable {
		private static final long serialVersionUID = -6823011111281387872L;
		public enum status {MINIMAL, EXTENDABLE, FALSE, FAILED}
		private CompletionMessage() {}
		private status result;
	}
	
	/////////////////
	// Actor State //
	/////////////////
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private final Queue<Worker.WorkMessage> unassignedWork = new LinkedList<>();
	private final Queue<ActorRef> idleWorkers = new LinkedList<>();
    private final Map<ActorRef, Worker.WorkMessage> busyWorkers = new HashMap<>();

	private TaskMessage task;

	////////////////////
	// Actor Behavior //
	////////////////////
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RegistrationMessage.class, this::handle)
				.match(Terminated.class, this::handle)
				.match(TaskMessage.class, this::handle)
				.match(CompletionMessage.class, this::handle)
				.matchAny(object -> this.log.info("Received unknown message: \"{}\"", object.toString()))
				.build();
	}

	private void handle(RegistrationMessage message) {
		this.context().watch(this.sender());
		
		this.assign(this.sender());
		this.log.info("Registered {}", this.sender());
	}
	
	private void handle(Terminated message) {
		this.context().unwatch(message.getActor());
		
		if (!this.idleWorkers.remove(message.getActor())) {
            Worker.WorkMessage work = this.busyWorkers.remove(message.getActor());
			if (work != null) {
				this.assign(work);
			}
		}		
		this.log.info("Unregistered {}", message.getActor());
	}
	
	private void handle(TaskMessage message) {
		if (this.task != null)
			this.log.error("The profiler actor can process only one task in its current implementation!");
		
		this.task = message;
        this.assign(new Worker.WorkMessage(new int[0], new int[0]));
	}
	
	private void handle(CompletionMessage message) {
		ActorRef worker = this.sender();
        Worker.WorkMessage work = this.busyWorkers.remove(worker);

		this.log.info("Completed: [{},{}]", Arrays.toString(work.getX()), Arrays.toString(work.getY()));
		
		switch (message.getResult()) {
			case MINIMAL: 
				this.report(work);
				break;
			case EXTENDABLE:
				this.split(work);
				break;
			case FALSE:
				// Ignore
				break;
			case FAILED:
				this.assign(work);
				break;
		}
		
		this.assign(worker);
	}

    private void assign(Worker.WorkMessage work) {
		ActorRef worker = this.idleWorkers.poll();
		
		if (worker == null) {
			this.unassignedWork.add(work);
			return;
		}
		
		this.busyWorkers.put(worker, work);
		worker.tell(work, this.self());
	}
	
	private void assign(ActorRef worker) {
        Worker.WorkMessage work = this.unassignedWork.poll();
		
		if (work == null) {
			this.idleWorkers.add(worker);
			return;
		}
		
		this.busyWorkers.put(worker, work);
		worker.tell(work, this.self());
	}

    private void report(Worker.WorkMessage work) {
		this.log.info("UCC: {}", Arrays.toString(work.getX()));
	}

    private void split(Worker.WorkMessage work) {
		int[] x = work.getX();
		int[] y = work.getY();
		
		int next = x.length + y.length;
		
		if (next < this.task.getAttributes() - 1) {
			int[] xNew = Arrays.copyOf(x, x.length + 1);
			xNew[x.length] = next;
            this.assign(new Worker.WorkMessage(xNew, y));
			
			int[] yNew = Arrays.copyOf(y, y.length + 1);
			yNew[y.length] = next;
            this.assign(new Worker.WorkMessage(x, yNew));
		}
	}
}