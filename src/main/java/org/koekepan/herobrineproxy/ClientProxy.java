package org.koekepan.herobrineproxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.koekepan.herobrineproxy.session.ClientSession;
import org.koekepan.herobrineproxy.session.ClientProxySession;
import org.koekepan.herobrineproxy.session.IClientSession;
import org.koekepan.herobrineproxy.session.IProxySessionNew;
import org.koekepan.herobrineproxy.sps.*;

import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.server.ServerAdapter;
import com.github.steveice10.packetlib.event.server.SessionAddedEvent;
import com.github.steveice10.packetlib.event.server.SessionRemovedEvent;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;


// the main class for the proxy
// this class creates a proxy session for every client session that is connected

public class ClientProxy {


	//private ScheduledExecutorService serverPoll = Executors.newSingleThreadScheduledExecutor();

	private String ThisProxyHost = null;
	private int ThisProxyPort = 0;
	private String VastHost = null;
	private int VastPort = 0;
	private ISPSConnection spsConnection;
	
	private Server server = null;
	private Map<Session, IProxySessionNew> sessions = new HashMap<Session, IProxySessionNew>();
	

	public ClientProxy(final String ThisProxyHost, final int ThisProxyPort, final String VastHost, final int VastPort) {
		this.ThisProxyHost = ThisProxyHost;
		this.ThisProxyPort = ThisProxyPort;
		this.VastHost = VastHost;
		this.VastPort = VastPort;
		
		// setup proxy server and add listener to create and store/discard proxy sessions as clients connect/disconnect
		server = new Server(ThisProxyHost, ThisProxyPort, HerobrineProxyProtocol.class, new TcpSessionFactory());
		this.spsConnection = new SPSConnection(this.VastHost, this.VastPort);
		ConsoleIO.println("Connecting to VAST sps server");
		this.spsConnection.connect();

//		ConsoleIO.println("Starting to add new listener");

		server.addListener(new ServerAdapter() {
			
			@Override
			public void sessionAdded(SessionAddedEvent event) {
				Session session = event.getSession();
				ConsoleIO.println("HerobrineProxy::sessionAdded => A SessionAdded event occured from <"+session.getHost()+":"+session.getPort()+"> to server <"+VastHost+":"+VastPort+">");
				IClientSession clientSession = new ClientSession(session);
				IProxySessionNew proxySession = new ClientProxySession(clientSession, spsConnection, VastHost, VastPort);
				sessions.put(event.getSession(), proxySession);
			}

			@Override
			public void sessionRemoved(SessionRemovedEvent event) {
				sessions.remove(event.getSession()).disconnect();
			}
		});
	}

	
	// returns whether the proxy server is currently listening for client connections
	public boolean isListening() {
		return server != null && server.isListening();
	}


	// initializes the proxy
	public void bind() {
		server.bind(true);
	}

	
	// closes the proxy
	public void close() {
		server.close(true);
	}

	
	public String getThisProxyHost() {
		return ThisProxyHost;
	}

	
	public int getThisProxyPort() {
		return ThisProxyPort;
	}


	public String getVastHost() {
		return VastHost;
	}


	public int getVastPort() {
		return VastPort;
	}

	
	public List<IProxySessionNew> getSessions() {
		return new ArrayList<IProxySessionNew>(sessions.values());
	}	
}
