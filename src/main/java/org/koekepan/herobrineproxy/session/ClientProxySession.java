package org.koekepan.herobrineproxy.session;

import org.koekepan.herobrineproxy.ConsoleIO;
import org.koekepan.herobrineproxy.packet.behaviours.ClientSessionPacketBehaviours;
import org.koekepan.herobrineproxy.packet.behaviours.ServerSessionPacketBehaviours;
import org.koekepan.herobrineproxy.sps.*;

import com.github.steveice10.packetlib.packet.Packet;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;


public class ClientProxySession implements IProxySessionNew {

	IClientSession clientSession;
	IServerSession spsSession;
	IServerSession spsServerSession;
	IServerSession newServerSession;
	ISPSConnection spsConnection;
	
	ClientSessionPacketBehaviours clientPacketBehaviours;
	ServerSessionPacketBehaviours serverPacketBehaviours;
	ServerSessionPacketBehaviours newServerPacketBehaviours;

	// initialize these lists somewhere in your code
	private List<ChunkPosition> positions = new ArrayList<>();
	private List<ChunkPosition> isolatedPositions = new ArrayList<>();


	public IClientSession getClientSession() {
		return clientSession;
	}

	public ClientProxySession(IClientSession clientSession, ISPSConnection spsConnection, String serverHost, int serverPort) {
		this.clientSession = clientSession;
		this.spsConnection = spsConnection;
		this.spsSession = new SPSSession(spsConnection);
		
		this.clientPacketBehaviours = new ClientSessionPacketBehaviours(this, (SPSConnection) spsConnection);
		this.clientPacketBehaviours.registerDefaultBehaviours(clientSession);
		this.clientPacketBehaviours.registerForwardingBehaviour(spsSession);

//		this.serverPacketBehaviours = new ServerSessionPacketBehaviours(this, spsSession);
//		this.serverPacketBehaviours.registerDefaultBehaviours(clientSession);
//		this.serverPacketBehaviours.registerForwardingBehaviour();

		this.clientSession.setPacketBehaviours(clientPacketBehaviours);
//		this.spsSession.setPacketBehaviours(clientPacketBehaviours);

//		ConsoleIO.println("behaviours regi");

//		this.serverSession = new ServerSession(serverHost, serverPort);
	}
		
	@Override
	public String getUsername() {
		return clientSession.getUsername();
	}
	
	@Override
	public void setUsername(String username) {
		clientSession.setUsername(username);
		spsSession.setUsername(username);
	}
	
	@Override
	public void sendPacketToClient(Packet packet) {
		ConsoleIO.println("ClientProxySession::sendPacketToClient via Behaviors => Sending packet <"+packet.getClass().getSimpleName()+"> to client <"+clientSession.getHost()+":"+clientSession.getPort()+">");
		clientSession.sendPacket(packet);
	}
	
	@Override
	public void sendPacketToVastMatcher(Packet packet) { //TODO: This is used with packets that have behaviours, login packets use send function directly in SPSSession?
		ConsoleIO.println("ClientProxySession::sendPacketToVastMatcherServer via Behaviors => Stopped Sending packet <"+packet.getClass().getSimpleName()+"> to server <"+ spsSession.getHost()+":"+ spsSession.getPort()+">");

		spsSession.sendPacket(packet);
//		SPSPacket(Packet packet, String username, int x, int y, int radius, String channel) {

//		SPSPacket spsPacket = new SPSPacket(packet, "user_01", 2, 3, 2, "serverBound");
//		ConsoleIO.println("THE CHANNEL IS: " + spsPacket.channel);
//		this.spsConnection.publish(spsPacket); // Something like this
	}
	

	@Override
	public void setServerHost(String host) {
		// TODO Auto-generated method stub
	}

	
	@Override
	public void setServerPort(int port) {
		// TODO Auto-generated method stub
	}


	@Override
	public void connect(String host, int port) {
//		this.clientPacketBehaviours.registerForwardingBehaviour();
//		this.serverPacketBehaviours = new ServerSessionPacketBehaviours(this, serverSession);
////		this.serverPacketBehaviours.clearBehaviours();
//		this.serverPacketBehaviours.registerForwardingBehaviour();
////		this.serverPacketBehaviours.registerDefaultBehaviours();
//		this.serverSession.setPacketBehaviours(serverPacketBehaviours);	
//		serverSession.connect();
		ConsoleIO.println("ClientToSPSProxy::connect => Player <"+getUsername()+"> is connecting to server <"+host+":"+port+">");
//		this.clientPacketBehaviours.registerForwardingBehaviour();
//		this.serverPacketBehaviours = new ServerSessionPacketBehaviours(this, spsSession);
//		this.serverPacketBehaviours.registerForwardingBehaviour();
//		this.spsSession.setPacketBehaviours(serverPacketBehaviours);
//		this.clientSession.setPacketBehaviours(serverPacketBehaviours);
		spsSession.connect();

		this.spsConnection.addListener(this.clientSession);


	}


	
	
	@Override
	public boolean isConnected() {
		return spsSession.isConnected();
	}
	
	
	@Override
	public void disconnect() {
		disconnectFromServer();
		disconnectFromClient();
	}
	
	
	@Override
	public void disconnectFromServer() {
		if (isConnected() ) {
			spsSession.disconnect();
		}
	}
	
	
	private void disconnectFromClient() {
		if (clientSession.isConnected()) {
			clientSession.disconnect();
		}
	}




	@Override
	public String getServerHost() {
		return spsSession.getHost();
	}


	@Override
	public int getServerPort() {
		return spsSession.getPort();
	}


	@Override
	public void migrate(String host, int port) {
		ConsoleIO.println("ClientProxySession::migrate => Migrating player <"+getUsername()+"> to new server <"+host+":"+port+">");
		newServerSession = new ServerSession(getUsername(), host, port);
		this.newServerPacketBehaviours = new ServerSessionPacketBehaviours(this, newServerSession);
		this.newServerPacketBehaviours.registerMigrationBehaviour();
		this.newServerSession.setPacketBehaviours(newServerPacketBehaviours);	
		newServerSession.connect();
	}
	
	
	@Override
	public void switchServer() {
		spsSession = newServerSession;
		this.serverPacketBehaviours.clearBehaviours();
		this.serverPacketBehaviours = this.newServerPacketBehaviours;
		this.newServerPacketBehaviours = null;
	}
	
	
	@Override 
	public void setPacketForwardingBehaviour() {
//		this.serverPacketBehaviours.registerForwardingBehaviour(); // Disabled: see ClientSessionPacketBehaviours.java
	}
	
	
	@Override 
	public void registerForPluginChannels() {
		this.spsSession.registerClientForChannels();
	}

	public void receiveChunkPosition(ChunkPosition position) {
		synchronized(positions) {
			positions.add(position);
		}
//		updateIsolatedPositions();
	}

	public void updateIsolatedPositions() {
		synchronized(positions) {
			if (positions.toArray().length >= 441) { //TODO: This should be the setting the server admin set: https://gaming.stackexchange.com/questions/260324/what-exactly-is-affected-by-a-higher-view-distance-on-the-server-side
				isolatedPositions.clear();


				ChunkProcessor test = new ChunkProcessor();

				isolatedPositions = test.getPolygonCorners(positions);

//			if (isolatedPositions.toArray().length == 164) { //TODO will be square see above
//				/// ISOLATE 4 corners
//				int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
//				int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
//
//				for (ChunkPosition pos : isolatedPositions) {
//					minX = Math.min(minX, pos.getX());
//					maxX = Math.max(maxX, pos.getX());
//					minZ = Math.min(minZ, pos.getZ());
//					maxZ = Math.max(maxZ, pos.getZ());
//				}
//
//				Iterator<ChunkPosition> iterator = isolatedPositions.iterator();
//				while (iterator.hasNext()) {
//					ChunkPosition pos = iterator.next();
//					if (!((pos.getX() == minX || pos.getX2() == maxX) && (pos.getZ() == minZ || pos.getZ2() == maxZ))) {
//						iterator.remove();
//					}
//				}
				////
				SPSConnection _spsConnection = (SPSConnection) spsConnection;
				_spsConnection.subscribePolygon(new ArrayList<>(isolatedPositions));
//			}


				ConsoleIO.println("length of isolated positions: " + isolatedPositions.toArray().length);

			}
		}
	}

	private boolean isAdjacent(ChunkPosition a, ChunkPosition b) {
		return Math.abs(a.getX() - b.getX()) <= 3 && Math.abs(a.getZ() - b.getZ()) <= 3;
	}


	List<ChunkPosition> recentPositions = new ArrayList<>();

	public void removeChunkPosition(int x, int z) {
		// Add the current position to the recentPositions list
//		recentPositions.add(new ChunkPosition(x, z));

		// If the list exceeds 10 items, remove the oldest one
//		if(recentPositions.size() > 10) {
//			recentPositions.remove(0);
//		}
//		int tempcounter = 0;

		Boolean removed = false;
		synchronized(positions) {
			Iterator<ChunkPosition> iterator = positions.iterator();
			while (!removed) {
//				tempcounter += 1;
				while (iterator.hasNext()) {
					ChunkPosition position = iterator.next();
					// Remove the position if it matches the given x and z
					if (position.getX() == x && position.getZ() == z) {
						iterator.remove();
						removed = true;
						continue;
					}

//					if (tempcounter >= 10) {
//						removed = true;
//						ConsoleIO.println("reached 10, concerned");
//					}
					// Also remove the position if it is in the recentPositions list
//			for (ChunkPosition recentPosition : recentPositions) {
//				if (position.getX() == recentPosition.getX() && position.getZ() == recentPosition.getZ()) {
//					iterator.remove();
//					break;
//				}
//			}
				}
			}
			ConsoleIO.println("length of positions: " + positions.toArray().length);

//			updateIsolatedPositions();
		}


//		if (tempcounter % 5 == 0) {

//		}
	}

}

class ChunkProcessor {
	public List<ChunkPosition> getPolygonCorners(List<ChunkPosition> positions) {
		Set<ChunkPosition> corners = new HashSet<>();

		// Helper set for faster lookup
		Set<ChunkPosition> positionSet = new HashSet<>(positions);

		for (ChunkPosition chunk : positions) {
			// Check each side of the chunk for a neighboring chunk
			boolean hasLeft = positionSet.contains(new ChunkPosition(chunk.getX() - 16, chunk.getZ()));
			boolean hasRight = positionSet.contains(new ChunkPosition(chunk.getX() + 16, chunk.getZ()));
			boolean hasTop = positionSet.contains(new ChunkPosition(chunk.getX(), chunk.getZ() - 16));
			boolean hasBottom = positionSet.contains(new ChunkPosition(chunk.getX(), chunk.getZ() + 16));

			// If there's no chunk on the left, add the left corners
			if (!hasLeft) {
				corners.add(new ChunkPosition(chunk.getX(), chunk.getZ()));
				corners.add(new ChunkPosition(chunk.getX(), chunk.getZ() + 15));
			}

			// If there's no chunk on the right, add the right corners
			if (!hasRight) {
				corners.add(new ChunkPosition(chunk.getX() + 15, chunk.getZ()));
				corners.add(new ChunkPosition(chunk.getX() + 15, chunk.getZ() + 15));
			}

			// If there's no chunk on the top, add the top corners
			if (!hasTop) {
				corners.add(new ChunkPosition(chunk.getX(), chunk.getZ()));
				corners.add(new ChunkPosition(chunk.getX() + 15, chunk.getZ()));
			}

			// If there's no chunk on the bottom, add the bottom corners
			if (!hasBottom) {
				corners.add(new ChunkPosition(chunk.getX(), chunk.getZ() + 15));
				corners.add(new ChunkPosition(chunk.getX() + 15, chunk.getZ() + 15));
			}
		}

		// Return the list of unique corners
		return new ArrayList<>(corners);
	}
}
