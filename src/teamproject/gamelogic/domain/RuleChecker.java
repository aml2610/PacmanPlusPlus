package teamproject.gamelogic.domain;

import teamproject.constants.CellSize;
import teamproject.constants.CellState;
import teamproject.constants.GameOutcome;

/**
 * Check different rules, constraints and states
 *
 * @author aml
 *
 */
public class RuleChecker {

	/**
	 * Check cell validity.
	 *
	 * @param cell
	 *            the cell
	 * @return true, if successful
	 */
	public static boolean checkCellValidity(final Cell cell) {
		return cell.getPosition().getRow() >= 0 && cell.getPosition().getColumn() >= 0
				&& cell.getState() != CellState.OBSTACLE;

	}

	/**
	 * Check whether the player is trying to go outside of the map
	 *
	 * @param row
	 * @param column
	 *
	 * @return
	 */
	public static boolean isOutOfBounds(final int row, final int column) {
		return row > CellSize.Rows - 1 || column > CellSize.Columns - 1 || row < 0 || column < 0;
	}

	/**
	 * Check whether game should end
	 *
	 * @param game
	 * @return
	 */
	// TODO: possibly add the type as a game field instead of passing them
	// separately
	public static GameOutcome getGameOutcome(final Game game) {
		return game.getGameType().equals(GameType.SINGLEPLAYER) ? getSinglePlayerGameOutcome(game) : getMultiplayerGameOutcome(game);
	}

	/**
	 * Check whether a multiplayer game should end
	 * 
	 * @param game
	 * @return a GameOutcome enum value
	 */
	private static GameOutcome getMultiplayerGameOutcome(final Game game) {
		// TODO More complicated so will be implemented once networking is
		// integrated fully
		return GameOutcome.STILL_PLAYING;
	}

	/**
	 * Check whether a singleplayer game should end
	 * 
	 * @param game
	 * @return a GameOutcome enum value
	 */
	private static GameOutcome getSinglePlayerGameOutcome(final Game game) {
		final Cell[][] cells = game.getWorld().getMap().getCells();
		boolean ghostAtePlayer = false;
		boolean foodLeft = false;

		for (final Cell[] cellRow : cells) {
			for (int j = 0; j < cells[0].length; j++) {
				if(cellRow[j].getState().equals(CellState.FOOD)) {
					foodLeft = true;
					break;
				}
			}
		}
		
		for(final Ghost ghost : game.getWorld().getGhosts()) {
			if(ghost.getPosition().equals(game.getPlayer().getPosition())) {
				ghostAtePlayer  = true;
				break;
			}
		}

		if (!foodLeft) {
			return GameOutcome.LOCAL_PLAYER_WON;
		}

		if (ghostAtePlayer) {
			return GameOutcome.LOCAL_PLAYER_LOST;
		}

		return GameOutcome.STILL_PLAYING;
	}
}
