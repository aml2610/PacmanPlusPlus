package main.java.gamelogic.domain;

import main.java.event.Event;
import main.java.event.arguments.PlayerAbilityUsedEventArgs;
import main.java.event.listener.PlayerAbilityUsedListener;

/**
 * Represent a player's inventory
 */

/**
 * The player's skillset. Contains 3 items that will be bound to the Q,W and E
 * keys.
 *
 * @author Lyubomir Pashev
 * @author Simeon Kostadinov
 *
 */
public class RemoteSkillSet implements SkillSet {
	private Player owner;
	private Event<PlayerAbilityUsedListener, PlayerAbilityUsedEventArgs> onPlayerAbilityUsed;
	
	
	public RemoteSkillSet(Player owner) {
		this.owner = owner;
		onPlayerAbilityUsed = new Event<>((l, a) -> l.onPlayerAbilityUsed(a));
	}

	/**
	 * Use Q skill.
	 */
	@Override
	public void activateQ() {
		onPlayerAbilityUsed.fire(new PlayerAbilityUsedEventArgs(owner, 'q'));
	}

	/**
	 * Use W skill.
	 */
	@Override
	public void activateW() {
		onPlayerAbilityUsed.fire(new PlayerAbilityUsedEventArgs(owner, 'w'));
	}

	public Event<PlayerAbilityUsedListener, PlayerAbilityUsedEventArgs> getOnPlayerAbilityUsed() {
		return onPlayerAbilityUsed;
	}

	@Override
	public void incrementCooldown() {
		// nop
	}
}
