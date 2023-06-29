package org.koekepan.herobrineproxy.session;

import org.koekepan.herobrineproxy.ConsoleIO;
import org.koekepan.herobrineproxy.packet.behaviours.ClientSessionPacketBehaviours;
import org.koekepan.herobrineproxy.packet.behaviours.ServerSessionPacketBehaviours;
import org.koekepan.herobrineproxy.sps.*;

import com.github.steveice10.packetlib.packet.Packet;



public class ClientProxySession implements IProxySessionNew {

	IClientSession clientSession;
	IServerSession spsSession;
	IServerSession spsServerSession;
	IServerSession newServerSession;
	ISPSConnection spsConnection;
	
	ClientSessionPacketBehaviours clientPacketBehaviours;
	ServerSessionPacketBehaviours serverPacketBehaviours;
	ServerSessionPacketBehaviours newServerPacketBehaviours;
	
	
	public ClientProxySession(IClientSession clientSession, ISPSConnection spsConnection, String serverHost, int serverPort) {
		this.clientSession = clientSession;
		this.spsConnection = spsConnection;
		this.spsSession = new SPSSession(spsConnection);
		
		this.clientPacketBehaviours = new ClientSessionPacketBehaviours(this);
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
}
