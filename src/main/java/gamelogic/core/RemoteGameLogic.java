package main.java.gamelogic.core;

import main.java.constants.GameOutcome;
import main.java.event.Event;
import main.java.event.arguments.EntityChangedEventArgs;
import main.java.event.arguments.EntityMovedEventArgs;
import main.java.event.arguments.GameDisplayInvalidatedEventArgs;
import main.java.event.arguments.GameEndedEventArgs;
import main.java.event.arguments.RemoteGameEndedEventArgs;
import main.java.event.listener.EntityAddedListener;
import main.java.event.listener.EntityMovedListener;
import main.java.event.listener.EntityRemovingListener;
import main.java.event.listener.GameDisplayInvalidatedListener;
import main.java.event.listener.GameEndedListener;
import main.java.event.listener.RemoteGameEndedListener;
import main.java.gamelogic.domain.Game;

public class RemoteGameLogic implements GameLogic, EntityAddedListener, EntityRemovingListener, EntityMovedListener,
		RemoteGameEndedListener {
	private Game game;
	private Event<GameDisplayInvalidatedListener, GameDisplayInvalidatedEventArgs> onGameDisplayInvalidated;
	private Event<GameEndedListener, GameEndedEventArgs> onGameEnded;

	public RemoteGameLogic(final Game game) {
		this.game = game;
		onGameDisplayInvalidated = new Event<>((l, a) -> l.onGameDisplayInvalidated(a));
		onGameEnded = new Event<>((l, a) -> l.onGameEnded(a));
		this.game.getWorld().getOnEntityAddedEvent().addListener(this);
		this.game.getWorld().getOnEntityRemovingEvent().addListener(this);
	}

	public Game getGame() {
		return game;
	}

	@Override
	public void gameStep(final int period) {
		game.getWorld().gameStep(game);
		invalidateDisplay();
	}

	private void invalidateDisplay() {
		onGameDisplayInvalidated.fire(new GameDisplayInvalidatedEventArgs(this));
	}

	@Override
	public Event<GameDisplayInvalidatedListener, GameDisplayInvalidatedEventArgs> getOnGameDisplayInvalidated() {
		return onGameDisplayInvalidated;
	}

	@Override
	public void onEntityRemoving(final EntityChangedEventArgs args) {
		game.getWorld().getEntity(args.getEntityID()).getOnMovedEvent().removeListener(this);
	}

	@Override
	public void onEntityAdded(final EntityChangedEventArgs args) {
		game.getWorld().getEntity(args.getEntityID()).getOnMovedEvent().addListener(this);
	}

	@Override
	public void onEntityMoved(final EntityMovedEventArgs args) {
		invalidateDisplay();
	}

	@Override
	public Event<GameEndedListener, GameEndedEventArgs> getOnGameEnded() {
		return onGameEnded;
	}

	private void onGameEnded(final GameOutcome outcome) {
		game.setEnded();
		onGameEnded.fire(new GameEndedEventArgs(this, outcome));
	}

	@Override
	public void onRemoteGameEnded(final RemoteGameEndedEventArgs args) {
		onGameEnded(args.getOutcome());
	}
}
