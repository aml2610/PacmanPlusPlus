package teamproject.networking.integration;

import java.util.logging.Level;
import java.util.logging.Logger;

import teamproject.constants.CellState;
import teamproject.event.Event;
import teamproject.event.arguments.EntityMovedEventArgs;
import teamproject.event.arguments.MultiplayerGameStartingEventArgs;
import teamproject.event.arguments.PlayerMovedEventArgs;
import teamproject.event.listener.GameStartedListener;
import teamproject.event.listener.RemoteEntityUpdatedListener;
import teamproject.event.listener.MultiplayerGameStartingListener;
import teamproject.gamelogic.core.Lobby;
import teamproject.gamelogic.core.LobbyPlayerInfo;
import teamproject.gamelogic.domain.Entity;
import teamproject.gamelogic.domain.Game;
import teamproject.gamelogic.domain.GameSettings;
import teamproject.gamelogic.domain.GameType;
import teamproject.gamelogic.domain.LocalPlayer;
import teamproject.gamelogic.domain.Position;
import teamproject.gamelogic.domain.RemoteGhost;
import teamproject.gamelogic.domain.RemotePlayer;
import teamproject.gamelogic.domain.World;
import teamproject.networking.ClientManager;
import teamproject.networking.StandardClientManager;
import teamproject.networking.data.Packet;
import teamproject.networking.event.ClientDisconnectedListener;
import teamproject.networking.event.ClientTrigger;
import teamproject.networking.socket.Client;
import teamproject.ui.GameUI;

public class ClientInstance implements Runnable, ClientTrigger ,
		ClientDisconnectedListener, RemoteEntityUpdatedListener, GameStartedListener {
	private Client client;
	private String serverAddress;
	private ClientManager manager;
	private Game game;
	private Lobby lobby;
	private String username;
	private GameUI gameUI;
	private boolean alreadyDoneHandshake;
	private Logger logger = Logger.getLogger("network-client");
	private Event<MultiplayerGameStartingListener, MultiplayerGameStartingEventArgs> multiplayerGameStartingEvent;
	
	/**
	 * Creates a new client instance which, when ran, will connect to the server
	 * with the specified IP address.
	 * 
	 * Any objects which will be involved with networking (eg. the map, the game,
	 * etc.) should be passed in to this {@link ClientInstance} object via the
	 * constructor. This, in turn, will pass those objects into the appropriate
	 * triggers (so packets received from the network will update the local game
	 * state accordingly), and also register the created {@link ClientDispatcher}
	 * object as a listener to any local events (eg. player moved) which must be
	 * transmitted over the network.
	 * 
	 * @param serverAddress The IP address of the server to connect to.
	 */
	public ClientInstance(GameUI gameUI, String username, String serverAddress) {
		this.username = username;
		this.serverAddress = serverAddress;
		this.gameUI = gameUI;
		this.alreadyDoneHandshake = false;
		this.lobby = new Lobby();
		this.gameUI.setLobby(this.lobby);
		this.game = null;
		this.multiplayerGameStartingEvent = new Event<>((l, a) -> l.onMultiplayerGameStarting(a));
		
		logger.setLevel(Level.FINEST);
	}
	
	@Override
	public void run() {
		// Create the client socket object
		this.client = this.createClient(this.serverAddress);
		this.client.getDisconnectedEvent().addListener(this);
		
		// Create 
		this.manager = new StandardClientManager(client);
		this.manager.setTrigger(this);
		
		addGameHooks();
		
		client.start();
	}
	
	public boolean isHost() {
		return client.getClientID() == 0;
	}
	
	/**
	 * This kills the connection to the server.
	 * 
	 * This relies on the client's disconnect event being fired in order
	 * to remove the game hooks in {@link this#onClientDisconnected(int)}.
	 */
	public void stop() {
		client.die();
	}

	@Override
	public void onClientDisconnected(int clientID) {
		removeGameHooks();
	}
	
	/**
	 * Creates the client object connecting to the server with the specified
	 * IP address.
	 * 
	 * @param ip The IP address of the server to connect to.
	 * @return The client object which, when started, will connect to the
	 * specified server.
	 */
	private Client createClient(String ip) {
		Client client = new Client(ip);
		
		return client;
	}
	
	/**
	 * This method adds the client dispatcher as a listener to any local game events
	 * which must be transmitted over the network. The dispatcher will listen to
	 * those events, transform them into a packet representing the same info, and then
	 * send them to the current client object, which sends the packets to the server.
	 * 
	 * See the comment at the top of the method for a (fictitious) example of how this
	 * would be done for a "local player moved" game event.
	 */
	private void addGameHooks() {
		/* Example of how a local player moved event would be handled.
		 * Assume the game world object was passed to the ClientInstance via the
		 * constructor.
		 * 
		 * this.gameWorld.getPlayerMovedEvent().addListener(this);
		 * 
		 * This instance object would then implement the PlayerMovedListener interface,
		 * such that the onPlayerMoved method transforms any of the relevant data (eg.
		 * X and Y co-ordinates) and turns it into a packet, which is then dispatched
		 * to the client manager, which turns the packet into bytes and sends it to
		 * the server.
		 */
	}
	
	private void addWorldGameHooks(Game game) {
		game.getPlayer().getOnMovedEvent().addListener(this);
	}
	
	/**
	 * In order to prevent resource leaks and to stop the event listener lists growing
	 * increasingly large as the player joins successive online games, any of the game
	 * hooks registered in {@link this#addGameHooks()} should be removed in the exact
	 * same manner they were registered.
	 * 
	 * This is also necessary so that game events are not passed to a client manager
	 * for a connection which has since been terminated.
	 */
	private void removeGameHooks() {
		/* Corresponding example for the local player moved event (see above).
		 * 
		 * this.gameWorld.getPlayerMovedEvent().removeListener(this);
		 */
		
		if(game != null) removeWorldGameHooks(game);
	}
	
	private void removeWorldGameHooks(Game game) {
		game.getPlayer().getOnMovedEvent().removeListener(this);
	}
	
	public Event<MultiplayerGameStartingListener, MultiplayerGameStartingEventArgs> getMultiplayerGameStartingEvent() {
		return multiplayerGameStartingEvent;
	}

	/* HANDLERS to create/deal with outgoing packets */
	@Override
	public void onEntityMoved(EntityMovedEventArgs args) {
		if(args.getEntity() instanceof LocalPlayer) {
			Packet p = new Packet("player-moved");
			p.setInteger("row", args.getRow());
			p.setInteger("col", args.getCol());
			if(args instanceof PlayerMovedEventArgs) {
				p.setDouble("angle", ((PlayerMovedEventArgs) args).getAngle());
			}
			logger.log(Level.INFO, "sending player moved packet");
			manager.dispatch(p);
		}
	}

	/* TRIGGERS to deal with incoming packets */
	@Override
	public void trigger(Packet p) {
		logger.log(Level.INFO, "Packet received: {0}", p.getPacketName());
		if(p.getPacketName().equals("server-handshake")) {
			triggerHandshake(p);
		} else if(p.getPacketName().equals("remote-player-moved")) {
			triggerRemotePlayerMoved(p);
		} else if(p.getPacketName().equals("remote-ghost-moved")) {
			triggerRemoteGhostMoved(p);
		} else if(p.getPacketName().equals("game-tile-changed")) {
			triggerGameTileChanged(p);
		} else if(p.getPacketName().equals("remote-ghost-joined")) {
			triggerRemoteGhostJoined(p);
		} else if(p.getPacketName().equals("remote-player-joined")) {
			triggerRemotePlayerJoined(p);
		} else if(p.getPacketName().equals("remote-player-left")) {
			triggerRemotePlayerLeft(p);
		} else if(p.getPacketName().equals("remote-ghost-left")) {
			triggerRemoteGhostLeft(p);
		} else if(p.getPacketName().equals("lobby-player-enter")) {
			triggerLobbyPlayerEnter(p);
		} else if(p.getPacketName().equals("lobby-player-left")) {
			triggerLobbyPlayerLeave(p);
		} else if(p.getPacketName().equals("lobby-rule-display-changed")) {
			triggerLobbyRuleDisplayChanged(p);
		} else if(p.getPacketName().equals("game-starting")) {
			triggerGameStarting(p);
		} else if(p.getPacketName().equals("force-move")) {
			triggerForceMove(p);
		}
	}

	private void triggerForceMove(Packet p) {
		int row = p.getInteger("row"), col = p.getInteger("col");
		double angle = p.getDouble("angle");
		
		game.getPlayer().setPosition(new Position(row, col));
		game.getPlayer().setAngle(angle);
	}

	private void triggerGameStarting(Packet p) {
		GameSettings settings = new GameSettings();
		// reconstruct game settings as needed
		

		MultiplayerGameStartingEventArgs args = new MultiplayerGameStartingEventArgs(settings, client.getClientID(), username);
		
		this.getMultiplayerGameStartingEvent().fire(args);
	}

	private void triggerLobbyPlayerLeave(Packet p) {
		int playerID = p.getInteger("player-id");
		
		lobby.removePlayer(playerID);
	}

	private void triggerLobbyRuleDisplayChanged(Packet p) {
		int size = p.getInteger("rule-strings.length");
		String[] s = new String[size];
		
		for(int i = 0; i < size; i++) {
			s[i] = p.getString("rule-strings[" + i + "]");
		}
		
		lobby.setSettingsString(s);
	}

	private void triggerLobbyPlayerEnter(Packet p) {
		int playerID = p.getInteger("player-id");
		String playerName = p.getString("player-name");
		
		LobbyPlayerInfo lobbyPlayerInfo = new LobbyPlayerInfo(playerID, playerName); 
		
		lobby.addPlayer(playerID, lobbyPlayerInfo);
	}

	private void triggerRemoteGhostLeft(Packet p) {
		int ghostID = p.getInteger("ghost-id");
		
		game.getWorld().removeEntity(ghostID);
	}

	private void triggerRemoteGhostJoined(Packet p) {
		int ghostID = p.getInteger("ghost-id");
		
		RemoteGhost ghost = new RemoteGhost(ghostID);
		ghost.setPosition(new Position(p.getInteger("row"), p.getInteger("col")));
		game.getWorld().addEntity(ghost);
	}

	private void triggerRemotePlayerJoined(Packet p) {
		int playerID = p.getInteger("player-id");
		String name = p.getString("name");

		RemotePlayer player = new RemotePlayer(playerID, name);
		player.setPosition(new Position(p.getInteger("row"), p.getInteger("col")));
		
		game.getWorld().addEntity(player);
	}

	private void triggerRemotePlayerLeft(Packet p) {
		int playerID = p.getInteger("player-id");
		game.getWorld().removeEntity(playerID);
	}

	private void triggerRemotePlayerMoved(Packet p) {
		int row = p.getInteger("row"), col = p.getInteger("col");
		int playerID = p.getInteger("player-id");
		
		Entity e = game.getWorld().getEntity(playerID);
		logger.log(Level.INFO, "remoteplayer");
		
		if(e instanceof RemotePlayer) {
			RemotePlayer player = (RemotePlayer)e;
			player.setPosition(new Position(row, col));
			if(p.hasParameter("angle"))
				player.setAngle(p.getDouble("angle"));
		} else {
			throw new RuntimeException("wut"); // TODO: error reporting
		}
	}

	private void triggerGameTileChanged(Packet p) {
		int x = p.getInteger("x"), y = p.getInteger("y");
		CellState cellState = CellState.valueOf(p.getString("cell-state"));
		
		game.getWorld().getMap().getCell(x, y).setState(cellState);
	}

	private void triggerRemoteGhostMoved(Packet p) {
		int row = p.getInteger("row"), col = p.getInteger("col");
		int ghostID = p.getInteger("ghost-id");
		
		Entity e = game.getWorld().getEntity(ghostID);
		
		if(e instanceof RemoteGhost) {
			RemoteGhost ghost = (RemoteGhost)e;
			ghost.setPosition(new Position(row, col));	
		} else {
			throw new RuntimeException("wut"); // TODO: error reporting
		}
	}

	private void triggerHandshake(Packet p) {
		if(!alreadyDoneHandshake) {
			int clientID = p.getInteger("client-id");
			client.setClientID(clientID);
			this.alreadyDoneHandshake = true;
			
			Packet p2 = new Packet("client-handshake");
			p2.setString("username", username);
			manager.dispatch(p2);
		} else {
			throw new RuntimeException("Already received handshake packet from server.");
		}
	}

	@Override
	public void onGameStarted(Game game) {
		if(game.getGameType() == GameType.MULTIPLAYER_CLIENT) {
			if(this.game != null) {
				removeWorldGameHooks(game);
			}
			this.game = game;
			addWorldGameHooks(game);
		}
	}
}
