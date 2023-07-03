package org.koekepan.herobrineproxy.packet.behaviours.server;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUnloadChunkPacket;
import com.github.steveice10.packetlib.packet.Packet;
import org.koekepan.herobrineproxy.behaviour.Behaviour;
import org.koekepan.herobrineproxy.session.ChunkPosition;
import org.koekepan.herobrineproxy.session.ClientProxySession;
import org.koekepan.herobrineproxy.session.IProxySessionNew;
import org.koekepan.herobrineproxy.session.IServerSession;

public class ServerUnloadChunkPacketBehaviour implements Behaviour<Packet> {
    private IProxySessionNew proxySession;
    private IServerSession serverSession;
    public ServerUnloadChunkPacketBehaviour(IProxySessionNew proxySession) {
        this.proxySession = proxySession;
    }

    public void process(Packet packet) {
        proxySession.sendPacketToClient(packet);

        ServerUnloadChunkPacket serverUnloadChunkPacket = (ServerUnloadChunkPacket) packet;

        int chunkX = serverUnloadChunkPacket.getX();
        int chunkZ = serverUnloadChunkPacket.getZ();

// Convert chunk coordinates to block coordinates
        int blockX = chunkX * 16;
        int blockZ = chunkZ * 16;

// Calculate the block coordinates of the four corners
        int x1 = blockX;
        int z1 = blockZ;
//        System.out.println("Packet received for unload-chunk with x,y: (" + x1 + ", " + z1 + ")");

        final ClientProxySession clientProxySession = (ClientProxySession) proxySession;
        clientProxySession.removeChunkPosition(x1, z1);
//        clientProxySession.removeChunkPosition(x2, z2);
//        clientProxySession.removeChunkPosition(x3, z3);
//        clientProxySession.removeChunkPosition(x4, z4);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                clientProxySession.updateIsolatedPositions();
//            }
//        }).start();
//        clientProxySession.updateIsolatedPositions();


    }

}