package org.koekepan.herobrineproxy.sps;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.koekepan.herobrineproxy.ConsoleIO;
import org.koekepan.herobrineproxy.HerobrineProxyProtocol;
//import org.koekepan.herobrineproxy.SPSServerProxy;
import org.koekepan.herobrineproxy.packet.EstablishConnectionPacket;
import org.koekepan.herobrineproxy.packet.PacketListener;
import org.koekepan.herobrineproxy.session.ChunkPosition;
import org.koekepan.herobrineproxy.session.IProxySessionConstructor;
import org.koekepan.herobrineproxy.session.IProxySessionNew;
import org.koekepan.herobrineproxy.session.ISession;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerMovementPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.login.client.LoginStartPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.io.buffer.ByteBufferNetInput;
import com.github.steveice10.packetlib.io.buffer.ByteBufferNetOutput;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.google.gson.Gson;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SPSConnection implements ISPSConnection {
	
	int x;
	int y;
	int radius = 1000;
	
	int SPSPort;
	String SPSHost;
	private Socket socket;
	int connectionID;
	private SPSProxyProtocol protocol;
	//private PacketProtocol protocol;
	private Map<String, ISession> listeners = new HashMap<String, ISession>();
	private IProxySessionConstructor sessionConstructor;
	private UUID uuid = UUID.randomUUID();

	public SPSConnection(String SPSHost, int SPSPort) {
		this.SPSHost = SPSHost;
		this.SPSPort = SPSPort;
		this.protocol = new SPSProxyProtocol();
	}
	
	
	public SPSConnection(String SPSHost, int SPSPort, IProxySessionConstructor sessionConstructor) {
		this(SPSHost, SPSPort);
		this.sessionConstructor = sessionConstructor;
	}


	private boolean initializeConnection() {
		final CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
		String URL = "http://"+this.SPSHost+":"+this.SPSPort;
		ConsoleIO.println(URL);
		final boolean[] result = {false};
		try {
			this.socket = IO.socket(URL);
			final UUID thisUUID = this.uuid;

			socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
				@Override
				public void call(Object... args) {
					socket.emit("handshake", thisUUID.toString() ,"Hello, server. This is Java Client");
				}
			}).on("handshake", new Emitter.Listener() {
				@Override
				public void call(Object... args) {
					if (("Hello, client with UUID: " + thisUUID.toString() + ". This is Node.js Server.").equals(args[0])) {
						System.out.println("Successfully connected to the correct server.");
						result[0] = true;
//						return true;
					} else {
						System.out.println("Failed to connect to the correct server.");
					}
					completableFuture.complete(true);
				}
			});

			socket.connect();

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		ConsoleIO.println("the result got from initializeconnection is: " + result[0]);

		try {
			result[0] = completableFuture.get();  // this will block until the CompletableFuture is complete
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		return result[0];
	}

	int tempcounter_deleteme = 0;

	public void initialiseListeners() {
		ConsoleIO.println("Initialize SPS listeners");
		socket.on("ID", new Emitter.Listener() {
			@Override
			public void call(Object... data) {
				receiveConnectionID((int) data[0]);
//				subscribe(0,0,0); // TODO: Check if this subscribe is necessary, what is it for?
			}
		});
		
		socket.on("getType", new Emitter.Listener() {
			
			@Override
			public void call(Object... args) {
				socket.emit("type", "client");
			}
		});
		
		socket.on("publication", new Emitter.Listener() {
			@Override
			public void call(Object... data) { // receive publication from vast matcher as a client
				tempcounter_deleteme += 1;
//				ConsoleIO.println("Received a publication from vastnet and attempting to send to Minecraft player!");
				SPSPacket packet = receivePublication(data);
				String username = packet.username;
				int x = packet.x;
				int y = packet.y;
				int radius = packet.radius;

				ConsoleIO.println("Amount of publications received: " + tempcounter_deleteme + ": " + packet.packet.getClass().getSimpleName());

				if (listeners.containsKey(username)) {
//					listeners.get(username).packetReceived(packet.packet);
//					ConsoleIO.println("It would also seem that the listener used is a: " + listeners.get(username).getClass().getName());
//					ConsoleIO.println("SPSConnection::publication => Sending packet <"+packet.packet.getClass().getSimpleName()+"> for player <"+username+"> at <"+x+":"+y+":"+radius+">");
					listeners.get(username).packetReceived(packet.packet);
				} else {
					ConsoleIO.println("SPSConnection::publication => Received a packet for an unknown session <"+username+">");
				}
			}
		});
	}


	@Override
	public void connect() {
		if (initializeConnection()) {
			initialiseListeners();
			initialiseVASTclient();
//			socket.connect();
		}
	}

	private void initialiseVASTclient() {
		ConsoleIO.println("Trying to spawn a VAST Client to represent the Minecraft Client on VASTnet");
		socket.emit("spawn_VASTclient", "Minecraft Client 1", "127.0.0.1", "20000", "100", "100");

		// TODO: This subscribe should be more client-specific:
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		socket.emit("subscribe", 100,100,100,"clientBound");
	}


	@Override
	public void disconnect() {
		ConsoleIO.println("SPS disconnected");
		socket.disconnect();
	}

	
	private Packet retrievePacket(String publication) {
		Gson gson = new Gson();
		Packet packet = null;
		try {
			//ConsoleIO.println(data[0].toString());
			byte[] payload = gson.fromJson(publication, byte[].class);
			packet = this.bytesToPacket(payload);
		} catch (Throwable e) {
			ConsoleIO.println(e.toString());
		}
		//ConsoleIO.println("SPSConnection::retrievePacket => Retrieved packet <"+packet.getClass().getSimpleName()+">");
		return packet;
	}
	
	
	@Override
	public SPSPacket receivePublication(Object... data) { // received from the vast matcher/client
		SPSPacket spsPacket = null;

		try {
			org.json.JSONObject jsonObject = (org.json.JSONObject) data[0];
			org.json.JSONObject payloadObject = jsonObject.getJSONObject("payload");

//			ConsoleIO.println("This is what I've got: " + payloadObject.toString());

//			{"chain":[1],"clientID":"zDJlt","payload":{"0":0,"1":"user_01","2":0,"3":0,"4":200,"5":"[117,7,117,115,101,114,95,48,49,1]","6":"login"},"recipients":[1],"channel":"login","aoi":{"center":{"x":500,"y":500},"radius":500},"matcherID":1}
			String connectionID = payloadObject.getString("connectionID");
			String userName = payloadObject.getString("username");
			int x = payloadObject.getInt("x");
			int y = payloadObject.getInt("y");
			int radius = payloadObject.getInt("radius");
			String publication = payloadObject.getString("actualPacket");
			String channel = payloadObject.getString("channel");

			Packet packet = retrievePacket(publication);

			spsPacket = new SPSPacket(packet, userName, x, y, radius, channel);
//				public SPSPacket(Packet packet, String username, int x, int y, int radius, String channel) {

			} catch (JSONException e) {
			e.printStackTrace();
			// Handle the exception (e.g., log it, throw it, etc.)
		}
		return spsPacket;
	}

	
	@Override
	public void receiveConnectionID(int connectionID) {
		this.connectionID = connectionID;
		ConsoleIO.println("Received connectionID: <"+connectionID+">");
	}
	
	
	int temp_pubcounter = 0;
//	int temp_movecounter = 0;

	int current_x = 100;
	int current_y = 100;

	@Override
	public void publish(SPSPacket packet) { // sends to vast matcher as client
//		ConsoleIO.println("Publish packet " + packet.toString());
		
		if ((packet.packet instanceof ClientPlayerPositionPacket)) {
			ConsoleIO.println("POSITION PACKET");
			double x = ((ClientPlayerMovementPacket) packet.packet).getX();
			double z = ((ClientPlayerMovementPacket) packet.packet).getZ();
			x = (int) x;
			z = (int) z;

			if (current_x != x || current_y != z) { // TODO: This is wack, fix:
				current_x = (int) x;
				current_y = (int) z;
				socket.emit("move", x, z); // TODO: Check if this is not too much move packets, alternatives?
//				socket.emit("clearsubscriptions", "clientBound");
//				subscribe(current_x, current_y, 10); // TODO: AOI
			}
		} else if ((packet.packet instanceof ClientPlayerPositionRotationPacket)) {
			ConsoleIO.println("POSITION PACKET");
			double x = ((ClientPlayerMovementPacket) packet.packet).getX();
			double z = ((ClientPlayerMovementPacket) packet.packet).getZ();
			x = (int) x;
			z = (int) z;
			if (current_x != x || current_y != z) { // TODO: This is wack, fix:
				current_x = (int) x;
				current_y = (int) z;
				socket.emit("move", x, z); // TODO: Check if this is not too much move packets, alternatives?
//				socket.emit("clearsubscriptions", "clientBound");
//				subscribe(current_x, current_y, 10); // TODO: AOI
			}
		}
		//convert to JSON
		Gson gson = new Gson();
		byte[] payload = this.packetToBytes(packet.packet);
		String json = gson.toJson(payload);
		//ConsoleIO.println("Connection <"+connectionID+"> sent packet <"+packet.packet.getClass().getSimpleName()+"> on channel <"+packet.channel+">");

		temp_pubcounter += 1;
		ConsoleIO.println("Amount of packets sent: " + temp_pubcounter + ": " + packet.packet.getClass().getSimpleName());
		socket.emit("publish", connectionID, packet.username, current_x, current_y, 0, json, packet.channel); // TODO: AOI - This should not be hard coded, this is also wack
	}


	@Override
	public void subscribe(int x, int z, int aoi) { // TODO: Fix subscription area
		socket.emit("subscribe", x, z, 10, "clientBound");
	} // TODO: AOI

	public void subscribePolygon(List<ChunkPosition> positions){
		List<float[]> posList = new ArrayList<float[]>();
		for (ChunkPosition position : positions) {
			posList.add(new float[]{position.getX(), position.getZ()});
		}

		String jsonPositions = new Gson().toJson(posList);

		ConsoleIO.println("Length of jsonpositions: " + jsonPositions.toString());

		socket.emit("clearsubscriptions", "clientBound");
		socket.emit("subscribe_polygon", jsonPositions, "clientBound");
	}

	@Override
	public void unsubscribed(String channel) {
		// TODO Auto-generated method stub
	}

		
	private byte[] packetToBytes(Packet packet) {
		ByteBuffer buffer = ByteBuffer.allocate(75000);
		ByteBufferNetOutput output = new ByteBufferNetOutput(buffer);
		
		int packetId = protocol.getOutgoingId(packet.getClass());
		try {
			protocol.getPacketHeader().writePacketId(output, packetId);
			packet.write(output);
		} catch (Exception e) {
			ConsoleIO.println("Exception: "+e.toString());
		}
		byte[] payload = new byte[buffer.position()];
		buffer.flip();
		buffer.get(payload);
		return payload;
	}
	
	
	private Packet bytesToPacket(byte[] payload) {
		ByteBuffer buffer = ByteBuffer.wrap(payload);
		ByteBufferNetInput input = new ByteBufferNetInput(buffer);
		Packet packet = null;
		try {
			int packetId = protocol.getPacketHeader().readPacketId(input);
		//	ConsoleIO.println("SPSConnection::byteToPacket => Protocol status <"+protocol.getSubProtocol().toString()+">");
			packet = protocol.createIncomingPacket(packetId);
			packet.read(input);
			
		} catch (Exception e) {
			ConsoleIO.println("Exception: "+e.toString());
		}
		return packet;
	}


	@Override
	public void addListener(ISession listener) {
		String username = listener.getUsername();
		listeners.put(username, listener);
	}
	
	
	@Override
	public void removeListener(ISession listener) {
		String username = listener.getUsername();
		listeners.remove(username);
	}


	@Override
	public String getHost() {
		return this.SPSHost;
	}


	@Override
	public int getPort() {
		return this.SPSPort;
	}
}
