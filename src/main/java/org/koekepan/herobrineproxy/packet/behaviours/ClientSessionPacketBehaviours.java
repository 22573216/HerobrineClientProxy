package org.koekepan.herobrineproxy.packet.behaviours;

import com.github.steveice10.mc.protocol.packet.ingame.server.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerDisplayScoreboardPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerScoreboardObjectivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerTeamPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.scoreboard.ServerUpdateScorePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.window.*;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.*;
import com.github.steveice10.mc.protocol.packet.login.server.EncryptionRequestPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginDisconnectPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginSetCompressionPacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginSuccessPacket;
import com.github.steveice10.mc.protocol.packet.status.server.StatusPongPacket;
import com.github.steveice10.mc.protocol.packet.status.server.StatusResponsePacket;
import org.koekepan.herobrineproxy.behaviour.BehaviourHandler;
import org.koekepan.herobrineproxy.packet.behaviours.client.ClientHandshakePacketBehaviour;
import org.koekepan.herobrineproxy.packet.behaviours.client.ClientLoginStartPacketBehaviour;
import org.koekepan.herobrineproxy.packet.behaviours.login.ServerLoginSuccessPacketBehaviour;
import org.koekepan.herobrineproxy.packet.behaviours.server.ServerChunkDataPacketBehaviour;
import org.koekepan.herobrineproxy.packet.behaviours.server.ServerJoinGamePacketBehaviour;
import org.koekepan.herobrineproxy.packet.behaviours.server.ServerPluginMessagePacketBehaviour;
import org.koekepan.herobrineproxy.packet.behaviours.server.ServerUnloadChunkPacketBehaviour;
import org.koekepan.herobrineproxy.session.IClientSession;
import org.koekepan.herobrineproxy.session.IProxySessionNew;

import com.github.steveice10.mc.protocol.packet.handshake.client.HandshakePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientRequestPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientResourcePackStatusPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientSettingsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientTabCompletePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerChangeHeldItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerMovementPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPlaceBlockPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerStatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerSwingArmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerUseItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientCloseWindowPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientConfirmTransactionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientCreativeInventoryActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientEnchantItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientSpectatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientSteerBoatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientSteerVehiclePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientUpdateSignPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientVehicleMovePacket;
import com.github.steveice10.mc.protocol.packet.login.client.EncryptionResponsePacket;
import com.github.steveice10.mc.protocol.packet.login.client.LoginStartPacket;
import com.github.steveice10.mc.protocol.packet.status.client.StatusPingPacket;
import com.github.steveice10.mc.protocol.packet.status.client.StatusQueryPacket;
import com.github.steveice10.packetlib.packet.Packet;
import org.koekepan.herobrineproxy.session.IServerSession;
import org.koekepan.herobrineproxy.sps.SPSConnection;

public class ClientSessionPacketBehaviours extends BehaviourHandler<Packet> {

	private SPSConnection spsConnection = null;
	private IClientSession clientSession;
	private IProxySessionNew proxySession;
	private ForwardPacketBehaviour serverForwarder;
	private ForwardPacketBehaviour clientForwarder;

	public ClientSessionPacketBehaviours(IProxySessionNew proxySession) {
		this.proxySession = proxySession;
	}

	public ClientSessionPacketBehaviours(IProxySessionNew proxySession, SPSConnection spsConnection) {
		this.proxySession = proxySession;
		this.spsConnection = spsConnection;
	}
	

	public void registerDefaultBehaviours(IClientSession clientSession) {
		this.clientSession = clientSession;
		clearBehaviours();
		registerBehaviour(HandshakePacket.class, new ClientHandshakePacketBehaviour(this.clientSession));										// 0x06 Player Position And Look
		registerBehaviour(LoginStartPacket.class, new ClientLoginStartPacketBehaviour(proxySession, spsConnection));												// 0x01 Login Start
	}
	
	
	public void registerForwardingBehaviour(IServerSession serverSession) {

		//////////////////////////////////// PACKETS THAT ARE SENT TO THE SERVER /////////////////////////////////////

		serverForwarder = new ForwardPacketBehaviour(proxySession, true);
		registerBehaviour(EncryptionResponsePacket.class, serverForwarder);
		registerBehaviour(ClientTeleportConfirmPacket.class, serverForwarder);
		registerBehaviour(ClientTabCompletePacket.class, serverForwarder);
		registerBehaviour(ClientChatPacket.class, serverForwarder);
		registerBehaviour(ClientRequestPacket.class, serverForwarder);
		registerBehaviour(ClientSettingsPacket.class, serverForwarder);
		registerBehaviour(ClientConfirmTransactionPacket.class, serverForwarder);
		registerBehaviour(ClientEnchantItemPacket.class, serverForwarder);
		registerBehaviour(ClientWindowActionPacket.class, serverForwarder);
		registerBehaviour(ClientCloseWindowPacket.class, serverForwarder);
		registerBehaviour(ClientPluginMessagePacket.class, serverForwarder);
		registerBehaviour(ClientPlayerInteractEntityPacket.class, serverForwarder);
		registerBehaviour(ClientKeepAlivePacket.class, serverForwarder);
		registerBehaviour(ClientPlayerPositionPacket.class, serverForwarder);
		registerBehaviour(ClientPlayerPositionRotationPacket.class, serverForwarder);
		registerBehaviour(ClientPlayerRotationPacket.class, serverForwarder);
		registerBehaviour(ClientPlayerMovementPacket.class, serverForwarder);
		registerBehaviour(ClientVehicleMovePacket.class, serverForwarder);
		registerBehaviour(ClientSteerBoatPacket.class, serverForwarder);
		registerBehaviour(ClientPlayerAbilitiesPacket.class, serverForwarder);
		registerBehaviour(ClientPlayerActionPacket.class, serverForwarder);
		registerBehaviour(ClientPlayerStatePacket.class, serverForwarder);
		registerBehaviour(ClientSteerVehiclePacket.class, serverForwarder);
		registerBehaviour(ClientResourcePackStatusPacket.class, serverForwarder);
		registerBehaviour(ClientPlayerChangeHeldItemPacket.class, serverForwarder);
		registerBehaviour(ClientCreativeInventoryActionPacket.class, serverForwarder);
		registerBehaviour(ClientUpdateSignPacket.class, serverForwarder);
		registerBehaviour(ClientPlayerSwingArmPacket.class, serverForwarder);
		registerBehaviour(ClientSpectatePacket.class, serverForwarder);
		registerBehaviour(ClientPlayerPlaceBlockPacket.class, serverForwarder);
		registerBehaviour(ClientPlayerUseItemPacket.class, serverForwarder);
		
		registerBehaviour(StatusQueryPacket.class, serverForwarder);
		registerBehaviour(StatusPingPacket.class, serverForwarder);

		//////////////////////////////////// PACKETS THAT ARE SENT TO THE CLIENT /////////////////////////////////////
		// TODO: This was never implimented like this, use to be registered to a packethandler registered with a spsSession, now everything is a clientsession.
		// NOTE: Check if this is the best way to do this, maybe bring back spssession packets.

		clientForwarder = new ForwardPacketBehaviour(proxySession, false);
		registerBehaviour(LoginDisconnectPacket.class, clientForwarder);
		registerBehaviour(EncryptionRequestPacket.class, clientForwarder);
		registerBehaviour(LoginSuccessPacket.class, new ServerLoginSuccessPacketBehaviour(proxySession));
		registerBehaviour(LoginSetCompressionPacket.class, clientForwarder);

		registerBehaviour(ServerSpawnObjectPacket.class, clientForwarder);
		registerBehaviour(ServerSpawnExpOrbPacket.class, clientForwarder);
		registerBehaviour(ServerSpawnGlobalEntityPacket.class, clientForwarder);
		registerBehaviour(ServerSpawnMobPacket.class, clientForwarder);
		registerBehaviour(ServerSpawnPaintingPacket.class, clientForwarder);
		registerBehaviour(ServerSpawnPlayerPacket.class, clientForwarder);
		registerBehaviour(ServerEntityAnimationPacket.class, clientForwarder);
		registerBehaviour(ServerStatisticsPacket.class, clientForwarder);
		registerBehaviour(ServerBlockBreakAnimPacket.class, clientForwarder);
		registerBehaviour(ServerUpdateTileEntityPacket.class, clientForwarder);
		registerBehaviour(ServerBlockValuePacket.class, clientForwarder);
		registerBehaviour(ServerBlockChangePacket.class, clientForwarder);
		registerBehaviour(ServerBossBarPacket.class, clientForwarder);
		registerBehaviour(ServerDifficultyPacket.class, clientForwarder);
		registerBehaviour(ServerTabCompletePacket.class, clientForwarder);
		registerBehaviour(ServerChatPacket.class, clientForwarder);
		registerBehaviour(ServerMultiBlockChangePacket.class, clientForwarder);
		registerBehaviour(ServerConfirmTransactionPacket.class, clientForwarder);
		registerBehaviour(ServerCloseWindowPacket.class, clientForwarder);
		registerBehaviour(ServerOpenWindowPacket.class, clientForwarder);
		registerBehaviour(ServerWindowItemsPacket.class, clientForwarder);
		registerBehaviour(ServerWindowPropertyPacket.class, clientForwarder);
		registerBehaviour(ServerSetSlotPacket.class, clientForwarder);
		registerBehaviour(ServerSetCooldownPacket.class, clientForwarder);
		registerBehaviour(ServerPlaySoundPacket.class, clientForwarder);
		registerBehaviour(ServerDisconnectPacket.class, clientForwarder);
		registerBehaviour(ServerEntityStatusPacket.class, clientForwarder);
		registerBehaviour(ServerExplosionPacket.class, clientForwarder);

		registerBehaviour(ServerUnloadChunkPacket.class, new ServerUnloadChunkPacketBehaviour(proxySession));
//		registerBehaviour(ServerUnloadChunkPacket.class, clientForwarder);

		registerBehaviour(ServerNotifyClientPacket.class, clientForwarder);
		registerBehaviour(ServerKeepAlivePacket.class, clientForwarder);


		registerBehaviour(ServerChunkDataPacket.class, new ServerChunkDataPacketBehaviour(proxySession));
//		registerBehaviour(ServerChunkDataPacket.class, clientForwarder);

		registerBehaviour(ServerPlayEffectPacket.class, clientForwarder);
		registerBehaviour(ServerSpawnParticlePacket.class, clientForwarder);
		registerBehaviour(ServerJoinGamePacket.class, new ServerJoinGamePacketBehaviour(proxySession, serverSession));
		registerBehaviour(ServerMapDataPacket.class, clientForwarder);
		registerBehaviour(ServerEntityPositionPacket.class, clientForwarder);
		registerBehaviour(ServerEntityPositionRotationPacket.class, clientForwarder);
		registerBehaviour(ServerEntityRotationPacket.class, clientForwarder);
		registerBehaviour(ServerEntityMovementPacket.class, clientForwarder);
		registerBehaviour(ServerVehicleMovePacket.class, clientForwarder);
		registerBehaviour(ServerOpenTileEntityEditorPacket.class, clientForwarder);
		registerBehaviour(ServerPlayerAbilitiesPacket.class, clientForwarder);
		registerBehaviour(ServerCombatPacket.class, clientForwarder);
		registerBehaviour(ServerPlayerListEntryPacket.class, clientForwarder);
		registerBehaviour(ServerPlayerUseBedPacket.class, clientForwarder);
		registerBehaviour(ServerEntityDestroyPacket.class, clientForwarder);
		registerBehaviour(ServerEntityRemoveEffectPacket.class, clientForwarder);
		registerBehaviour(ServerResourcePackSendPacket.class, clientForwarder);
		registerBehaviour(ServerRespawnPacket.class, clientForwarder);
		registerBehaviour(ServerEntityHeadLookPacket.class, clientForwarder);
		registerBehaviour(ServerWorldBorderPacket.class, clientForwarder);
		registerBehaviour(ServerSwitchCameraPacket.class, clientForwarder);
		registerBehaviour(ServerPlayerChangeHeldItemPacket.class, clientForwarder);
		registerBehaviour(ServerDisplayScoreboardPacket.class, clientForwarder);
		registerBehaviour(ServerEntityMetadataPacket.class, clientForwarder);
		registerBehaviour(ServerEntityAttachPacket.class, clientForwarder);
		registerBehaviour(ServerEntityVelocityPacket.class, clientForwarder);
		registerBehaviour(ServerEntityEquipmentPacket.class, clientForwarder);
		registerBehaviour(ServerPlayerPositionRotationPacket.class, clientForwarder);
		registerBehaviour(ServerPlayerSetExperiencePacket.class, clientForwarder);
		registerBehaviour(ServerPlayerHealthPacket.class, clientForwarder);
		registerBehaviour(ServerScoreboardObjectivePacket.class, clientForwarder);
		registerBehaviour(ServerEntitySetPassengersPacket.class, clientForwarder);
		registerBehaviour(ServerTeamPacket.class, clientForwarder);
		registerBehaviour(ServerUpdateScorePacket.class, clientForwarder);
		registerBehaviour(ServerSpawnPositionPacket.class, clientForwarder);
		registerBehaviour(ServerUpdateTimePacket.class, clientForwarder);
		registerBehaviour(ServerTitlePacket.class, clientForwarder);
		registerBehaviour(ServerPlayBuiltinSoundPacket.class, clientForwarder);
		registerBehaviour(ServerPlayerListDataPacket.class, clientForwarder);
		registerBehaviour(ServerEntityCollectItemPacket.class, clientForwarder);
		registerBehaviour(ServerEntityTeleportPacket.class, clientForwarder);
		registerBehaviour(ServerEntityPropertiesPacket.class, clientForwarder);
		registerBehaviour(ServerEntityEffectPacket.class, clientForwarder);

		registerBehaviour(StatusResponsePacket.class, clientForwarder);
		registerBehaviour(StatusPongPacket.class, clientForwarder);

		registerBehaviour(ServerPluginMessagePacket.class, new ServerPluginMessagePacketBehaviour(proxySession));
	}
}
