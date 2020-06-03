package com.codetaylor.mc.artisanworkstumps.modules.tanks.network;

import com.codetaylor.mc.athenaeum.util.RandomHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class SCPacketParticleCombust
    implements IMessage,
    IMessageHandler<SCPacketParticleCombust, IMessage> {

  private double x;
  private double y;
  private double z;
  private double rangeX;
  private double rangeY;
  private double rangeZ;

  @SuppressWarnings("unused")
  public SCPacketParticleCombust() {
    // serialization
  }

  public SCPacketParticleCombust(double x, double y, double z, double rangeX, double rangeY, double rangeZ) {

    this.x = x;
    this.y = y;
    this.z = z;
    this.rangeX = rangeX;
    this.rangeY = rangeY;
    this.rangeZ = rangeZ;
  }

  @Override
  public void toBytes(ByteBuf buf) {

    buf.writeDouble(this.x);
    buf.writeDouble(this.y);
    buf.writeDouble(this.z);
    buf.writeDouble(this.rangeX);
    buf.writeDouble(this.rangeY);
    buf.writeDouble(this.rangeZ);
  }

  @Override
  public void fromBytes(ByteBuf buf) {

    this.x = buf.readDouble();
    this.y = buf.readDouble();
    this.z = buf.readDouble();
    this.rangeX = buf.readDouble();
    this.rangeY = buf.readDouble();
    this.rangeZ = buf.readDouble();
  }

  @SideOnly(Side.CLIENT)
  @Override
  public IMessage onMessage(SCPacketParticleCombust message, MessageContext ctx) {

    World world = Minecraft.getMinecraft().world;

    for (int i = 0; i < 16; i++) {
      double offsetX = (RandomHelper.random().nextDouble() * 2.0 - 1.0) * message.rangeX;
      double offsetY = (RandomHelper.random().nextDouble() * 2.0 - 1.0) * message.rangeY;
      double offsetZ = (RandomHelper.random().nextDouble() * 2.0 - 1.0) * message.rangeZ;
      double x = message.x;
      double y = message.y;
      double z = message.z;
      world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + offsetX, y + offsetY, z + offsetZ, 0.0, 0.0, 0.0);
    }

    for (int i = 0; i < 4; i++) {
      double offsetX = (RandomHelper.random().nextDouble() * 2.0 - 1.0) * message.rangeX;
      double offsetY = (RandomHelper.random().nextDouble() * 2.0 - 1.0) * message.rangeY;
      double offsetZ = (RandomHelper.random().nextDouble() * 2.0 - 1.0) * message.rangeZ;
      double x = message.x;
      double y = message.y;
      double z = message.z;
      world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x + offsetX, y + offsetY, z + offsetZ, 0.0, 0.0, 0.0);
    }

    for (int i = 0; i < 16; i++) {
      double offsetX = (RandomHelper.random().nextDouble() * 2.0 - 1.0) * message.rangeX;
      double offsetY = (RandomHelper.random().nextDouble() * 2.0 - 1.0) * message.rangeY;
      double offsetZ = (RandomHelper.random().nextDouble() * 2.0 - 1.0) * message.rangeZ;
      double x = message.x;
      double y = message.y;
      double z = message.z;
      world.spawnParticle(EnumParticleTypes.FLAME, x + offsetX, y + offsetY, z + offsetZ, 0.0, 0.0, 0.0);
    }

    return null;
  }

}
